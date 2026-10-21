package com.sanedge.ecommerce.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sanedge.ecommerce.transaction.models.Transaction;
import com.sanedge.ecommerce.transaction.models.TransactionOutbox;
import com.sanedge.ecommerce.transaction.repository.TransactionOutboxRepository;

/**
 * The transaction events are written inside the transaction that creates the
 * transaction, so a failing outbox write has to abort that transaction instead
 * of dropping the event silently.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionCommandImplServiceOutboxTest {

    @Mock
    private TransactionOutboxRepository transactionOutboxRepository;

    private TransactionCommandImplService service;

    @BeforeEach
    void setUp() {
        // only the outbox repository is exercised by these tests
        service = new TransactionCommandImplService(null, null, transactionOutboxRepository,
                null, null, null, null, null, null);
    }

    private Transaction transaction() {
        Transaction tx = new Transaction();
        tx.setTransactionId(9L);
        tx.setOrderId(4);
        tx.setMerchantId(2);
        tx.setAmount(15000);
        tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return tx;
    }

    @Test
    void statsEventIsWrittenToTheOutbox() {
        service.publishStatsEvent(transaction());

        ArgumentCaptor<TransactionOutbox> captor = ArgumentCaptor.forClass(TransactionOutbox.class);
        verify(transactionOutboxRepository).enqueue(captor.capture());
        TransactionOutbox event = captor.getValue();
        assertThat(event.getTopic()).isEqualTo("stats.transaction.event");
        assertThat(event.getEventKey()).isEqualTo("2");
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getPayload()).contains("\"amount\":15000.0");
    }

    @Test
    void aFailingOutboxWriteAbortsTheTransaction() {
        doThrow(new IllegalStateException("db down")).when(transactionOutboxRepository).enqueue(any());

        assertThatThrownBy(() -> service.publishStatsEvent(transaction()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }
}
