package com.sanedge.ecommerce.common.outbox;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.sanedge.ecommerce.common.chaos.ChaosManager;
import com.sanedge.ecommerce.common.chaos.ChaosPolicy;

/**
 * Drains one module's outbox into Kafka.
 *
 * <p>Port of the blueprint's {@code AuthOutboxPublisher}: poll every second,
 * publish a batch, and on failure leave the row PENDING with an exponential
 * backoff. Two behaviour details are preserved deliberately:
 * <ul>
 *   <li>A single {@link AtomicBoolean} guard means a slow batch never overlaps
 *       with the next tick.</li>
 *   <li>Rows are claimed before sending, so multiple replicas cannot publish the
 *       same event twice.</li>
 * </ul>
 *
 * <p>Backoff is {@code min(300s, 1 << (attempts - 1))}, matching the blueprint.
 *
 * <p>Subclasses only supply the module-specific repository and a label.
 */
public abstract class KafkaOutboxPublisher<T extends OutboxEvent> {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxPublisher.class);

    private static final long MAX_BACKOFF_SECONDS = 300L;
    private static final int DEFAULT_BATCH_SIZE = 50;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChaosManager chaosManager;
    private final AtomicBoolean polling = new AtomicBoolean();

    protected KafkaOutboxPublisher(KafkaTemplate<String, String> kafkaTemplate, ChaosManager chaosManager) {
        this.kafkaTemplate = kafkaTemplate;
        this.chaosManager = chaosManager;
    }

    /** Module label used in log lines, e.g. {@code "auth"}. */
    protected abstract String moduleName();

    /** The module's outbox repository. */
    protected abstract OutboxEventRepository<T> repository();

    /** Batch size per tick; override to tune. */
    protected int batchSize() {
        return DEFAULT_BATCH_SIZE;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}", initialDelayString = "${app.outbox.initial-delay-ms:5000}")
    public void poll() {
        if (!polling.compareAndSet(false, true)) {
            return; // previous tick still running
        }
        try {
            publishDue();
        } catch (RuntimeException e) {
            log.error("{} outbox poll failed", moduleName(), e);
        } finally {
            polling.set(false);
        }
    }

    private void publishDue() {
        List<T> due = repository().findDue(Math.max(1, batchSize()));
        for (T event : due) {
            publishOne(event);
        }
    }

    /** Claim then publish. A lost claim means another replica owns this row. */
    private void publishOne(T event) {
        T claimed = repository().claim(event);
        if (claimed == null) {
            return;
        }

        try {
            send(claimed);
            repository().markSent(claimed);
        } catch (RuntimeException error) {
            log.warn("{} outbox publish failed event_id={} topic={} attempts={}",
                    moduleName(), claimed.getEventId(), claimed.getTopic(), claimed.getAttempts(), error);
            repository().markRetry(claimed, error, retryDelaySeconds(claimed.getAttempts()));
        }
    }

    /**
     * Publishes the payload, honouring {@code kafka}-type chaos policies so the
     * drop/reject knobs behave exactly as they did in the blueprint.
     */
    private void send(T event) {
        ChaosPolicy policy = chaosManager == null ? null : chaosManager.evaluate("kafka", event.getTopic());
        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
            log.info("Injecting Kafka chaos policy={} topic={}", policy.getName(), event.getTopic());

            if (policy.getLatencyMs() > 0) {
                try {
                    Thread.sleep(policy.getLatencyMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (policy.isDropMessage()) {
                throw new IllegalStateException("Simulated Kafka drop for topic " + event.getTopic());
            }
            if (policy.isRejectMessage()) {
                throw new IllegalStateException(policy.getErrorMessage() != null
                        ? policy.getErrorMessage()
                        : "Simulated Kafka rejection for topic " + event.getTopic());
            }
        }

        kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).join();
    }

    /** {@code min(300s, 1 << (attempts - 1))} — mirrors the blueprint. */
    private long retryDelaySeconds(int attempts) {
        return Math.min(MAX_BACKOFF_SECONDS, 1L << Math.min(8, Math.max(0, attempts - 1)));
    }
}
