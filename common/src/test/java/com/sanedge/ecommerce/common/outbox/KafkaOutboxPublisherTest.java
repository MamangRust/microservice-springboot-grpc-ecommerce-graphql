package com.sanedge.ecommerce.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import com.sanedge.ecommerce.common.chaos.ChaosManager;

/**
 * Unit tests for the shared outbox relay: claim before send, at-least-once
 * delivery and the exponential backoff.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaOutboxPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ChaosManager chaosManager;

    private TestRepository repository;

    private TestPublisher publisher;

    /** Minimal {@link OutboxEvent} implementation for the test. */
    static final class TestEvent implements OutboxEvent {
        private Long id;
        private String eventId = "event-1";
        private String topic = "stats.ecommerce.order.event";
        private String eventKey = "42";
        private String payload = "{}";
        private String status = "PENDING";
        private int attempts;
        private Timestamp nextAttemptAt;
        private Timestamp claimedAt;
        private String claimToken;
        private String lastError;

        @Override
        public Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String getEventKey() {
            return eventKey;
        }

        @Override
        public String getPayload() {
            return payload;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public int getAttempts() {
            return attempts;
        }

        @Override
        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        @Override
        public Timestamp getNextAttemptAt() {
            return nextAttemptAt;
        }

        @Override
        public void setNextAttemptAt(Timestamp nextAttemptAt) {
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public Timestamp getClaimedAt() {
            return claimedAt;
        }

        @Override
        public void setClaimedAt(Timestamp claimedAt) {
            this.claimedAt = claimedAt;
        }

        @Override
        public String getClaimToken() {
            return claimToken;
        }

        @Override
        public void setClaimToken(String claimToken) {
            this.claimToken = claimToken;
        }

        @Override
        public String getLastError() {
            return lastError;
        }

        @Override
        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
    }

    /** In-memory repository that records the calls of the relay. */
    static final class TestRepository implements OutboxEventRepository<TestEvent> {
        private final List<TestEvent> due = new ArrayList<>();
        private final List<TestEvent> sent = new ArrayList<>();
        private final List<TestEvent> retried = new ArrayList<>();
        private TestEvent claimResult;
        private long retryDelaySeconds = -1;
        private boolean failOnFind;

        @Override
        public List<TestEvent> findDue(int limit) {
            if (failOnFind) {
                throw new IllegalStateException("db down");
            }
            return due;
        }

        @Override
        public TestEvent claim(TestEvent event) {
            return claimResult;
        }

        @Override
        public void markSent(TestEvent event) {
            sent.add(event);
        }

        @Override
        public void markRetry(TestEvent event, Throwable failure, long delaySeconds) {
            retried.add(event);
            retryDelaySeconds = delaySeconds;
        }

        @Override
        public long countPending() {
            return due.size();
        }
    }

    static final class TestPublisher extends KafkaOutboxPublisher<TestEvent> {
        private final OutboxEventRepository<TestEvent> repository;

        TestPublisher(OutboxEventRepository<TestEvent> repository,
                KafkaTemplate<String, String> kafkaTemplate,
                ChaosManager chaosManager) {
            super(kafkaTemplate, chaosManager);
            this.repository = repository;
        }

        @Override
        protected String moduleName() {
            return "test";
        }

        @Override
        protected OutboxEventRepository<TestEvent> repository() {
            return repository;
        }
    }

    @BeforeEach
    void setUp() {
        repository = new TestRepository();
        publisher = new TestPublisher(repository, kafkaTemplate, chaosManager);
    }

    @Test
    void publishesClaimedRowsAndMarksThemSent() {
        TestEvent due = new TestEvent();
        due.setId(1L);
        TestEvent claimed = new TestEvent();
        claimed.setId(1L);
        claimed.setClaimToken("token-1");
        claimed.setAttempts(1);

        repository.due.add(due);
        repository.claimResult = claimed;
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.poll();

        verify(kafkaTemplate).send("stats.ecommerce.order.event", "42", "{}");
        assertThat(repository.sent).containsExactly(claimed);
        assertThat(repository.retried).isEmpty();
    }

    @Test
    void doesNotPublishRowsOwnedByAnotherReplica() {
        TestEvent due = new TestEvent();
        due.setId(2L);
        repository.due.add(due);
        repository.claimResult = null;

        publisher.poll();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        assertThat(repository.sent).isEmpty();
    }

    @Test
    void retriesWithExponentialBackoffWhenKafkaFails() {
        TestEvent due = new TestEvent();
        due.setId(3L);
        TestEvent claimed = new TestEvent();
        claimed.setId(3L);
        claimed.setClaimToken("token-3");
        claimed.setAttempts(3);

        repository.due.add(due);
        repository.claimResult = claimed;
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("broker down"));

        publisher.poll();

        assertThat(repository.sent).isEmpty();
        assertThat(repository.retried).containsExactly(claimed);
        // attempts = 3 -> backoff of 4 seconds
        assertThat(repository.retryDelaySeconds).isEqualTo(4L);
    }

    @Test
    void aFailingPollDoesNotKillTheRelay() {
        repository.failOnFind = true;

        publisher.poll();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
