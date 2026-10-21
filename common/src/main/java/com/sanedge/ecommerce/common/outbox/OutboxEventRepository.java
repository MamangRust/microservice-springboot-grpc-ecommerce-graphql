package com.sanedge.ecommerce.common.outbox;

import java.util.List;

/**
 * Per-module outbox persistence operations needed by {@link KafkaOutboxPublisher}.
 *
 * <p>Declared as an interface because each module has its own table and entity,
 * so the queries cannot live in {@code common}. Implementations should mirror the
 * Quarkus blueprint's {@code AuthOutboxRepository} semantics:
 * <ul>
 *   <li>{@code findDue} returns PENDING rows whose backoff has elapsed and whose
 *       lease has expired, oldest first, capped at {@code limit}.</li>
 *   <li>{@code claim} atomically takes a lease and increments {@code attempts},
 *       returning {@code null} when another node won the race.</li>
 *   <li>{@code markSent} only succeeds while this node still holds the lease.</li>
 *   <li>{@code markRetry} releases the lease and schedules the next attempt.</li>
 * </ul>
 */
public interface OutboxEventRepository<T extends OutboxEvent> {

    /** Rows ready to publish, oldest first. */
    List<T> findDue(int limit);

    /** Atomically leases one row; returns null if it was taken by someone else. */
    T claim(T event);

    /** Marks the event published. */
    void markSent(T event);

    /** Releases the lease and defers the next attempt by {@code delaySeconds}. */
    void markRetry(T event, Throwable failure, long delaySeconds);

    /** Number of rows still PENDING, for the backlog gauge. */
    long countPending();
}
