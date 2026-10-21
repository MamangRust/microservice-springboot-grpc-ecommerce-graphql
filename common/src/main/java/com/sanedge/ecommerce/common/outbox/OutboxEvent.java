package com.sanedge.ecommerce.common.outbox;

/**
 * Minimal contract every per-module outbox entity implements.
 *
 * <p>Each module owns its own table ({@code auth_outbox}, {@code transaction_outbox},
 * {@code merchant_outbox}, …) so the entity/repository stay module-local, but the
 * relay loop in {@link KafkaOutboxPublisher} only needs these fields.
 *
 * <p>Status values mirror the migration schema:
 * {@code PENDING} (queued, possibly retrying) and {@code SENT} (published).
 */
public interface OutboxEvent {

    /** Returns the database id, used by the claim/mark queries. */
    Long getId();

    String getEventId();

    /** Destination Kafka topic. */
    String getTopic();

    /** Kafka record key (the recipient email in the email flows). */
    String getEventKey();

    /** Serialized envelope published verbatim to Kafka. */
    String getPayload();

    String getStatus();

    void setStatus(String status);

    int getAttempts();

    void setAttempts(int attempts);

    java.sql.Timestamp getNextAttemptAt();

    void setNextAttemptAt(java.sql.Timestamp nextAttemptAt);

    java.sql.Timestamp getClaimedAt();

    void setClaimedAt(java.sql.Timestamp claimedAt);

    String getClaimToken();

    void setClaimToken(String claimToken);

    String getLastError();

    void setLastError(String lastError);
}
