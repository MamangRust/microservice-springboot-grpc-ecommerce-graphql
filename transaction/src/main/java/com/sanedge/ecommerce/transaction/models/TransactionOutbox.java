package com.sanedge.ecommerce.transaction.models;

import java.sql.Timestamp;

import com.sanedge.ecommerce.common.models.BaseModel;
import com.sanedge.ecommerce.common.outbox.OutboxEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Transactional outbox row for transaction email events (table
 * {@code transaction_outbox}, migration V23).
 *
 * <p>Written in the same transaction as the transaction insert, then drained by
 * {@link com.sanedge.ecommerce.transaction.service.TransactionOutboxPublisher}.
 * Columns mirror the migration and the Quarkus blueprint's entity exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "transaction_outbox")
public class TransactionOutbox extends BaseModel implements OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "event_key", length = 200)
    private String eventKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Timestamp nextAttemptAt;

    @Column(name = "claimed_at")
    private Timestamp claimedAt;

    @Column(name = "claim_token", length = 100)
    private String claimToken;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
