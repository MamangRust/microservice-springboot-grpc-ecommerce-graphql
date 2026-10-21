package com.sanedge.ecommerce.auth.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.ecommerce.auth.models.AuthOutbox;
import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Outbox persistence for auth email events.
 *
 * <p>Spring port of the blueprint's {@code AuthOutboxRepository}. The reactive
 * Panache queries become JPQL updates inside {@code @Transactional} methods; the
 * lease semantics (and therefore the exactly-once-ish guarantees across
 * replicas) are unchanged:
 * <ul>
 *   <li>{@code claim} only wins while the row is PENDING, unleased or
 *       lease-expired, and past its backoff — the returned row count decides.</li>
 *   <li>{@code markSent}/{@code markRetry} additionally check {@code claimToken},
 *       so a node whose lease expired cannot clobber a newer owner.</li>
 * </ul>
 */
@Repository
public class AuthOutboxRepository implements OutboxEventRepository<AuthOutbox> {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${auth.outbox.lease.seconds:300}")
    private long leaseSeconds;

    /** Persists a new pending event; call inside the business transaction. */
    @Transactional
    public void enqueue(AuthOutbox event) {
        entityManager.persist(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthOutbox> findDue(int limit) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        return entityManager.createQuery(
                "SELECT e FROM AuthOutbox e "
                        + "WHERE e.status = 'PENDING' "
                        + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now) "
                        + "AND (e.claimedAt IS NULL OR e.claimedAt < :leaseCutoff) "
                        + "ORDER BY e.createdAt",
                AuthOutbox.class)
                .setParameter("now", now)
                .setParameter("leaseCutoff", leaseCutoff)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional
    public AuthOutbox claim(AuthOutbox event) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        String claimToken = UUID.randomUUID().toString();

        int updated = entityManager.createQuery(
                "UPDATE AuthOutbox e "
                        + "SET e.claimedAt = :now, e.claimToken = :token, e.attempts = e.attempts + 1 "
                        + "WHERE e.id = :id AND e.status = 'PENDING' "
                        + "AND (e.claimedAt IS NULL OR e.claimedAt < :leaseCutoff) "
                        + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)")
                .setParameter("now", now)
                .setParameter("token", claimToken)
                .setParameter("id", event.getId())
                .setParameter("leaseCutoff", leaseCutoff)
                .executeUpdate();

        return updated > 0 ? entityManager.find(AuthOutbox.class, event.getId()) : null;
    }

    @Override
    @Transactional
    public void markSent(AuthOutbox event) {
        entityManager.createQuery(
                "UPDATE AuthOutbox e SET e.status = 'SENT', e.claimedAt = NULL, e.lastError = NULL "
                        + "WHERE e.id = :id AND e.status = 'PENDING' AND e.claimToken = :token")
                .setParameter("id", event.getId())
                .setParameter("token", event.getClaimToken())
                .executeUpdate();
    }

    @Override
    @Transactional
    public void markRetry(AuthOutbox event, Throwable failure, long delaySeconds) {
        entityManager.createQuery(
                "UPDATE AuthOutbox e SET e.status = 'PENDING', e.claimedAt = NULL, "
                        + "e.lastError = :error, e.nextAttemptAt = :next "
                        + "WHERE e.id = :id AND e.status = 'PENDING' AND e.claimToken = :token")
                .setParameter("error", failure == null ? "unknown failure" : failure.getMessage())
                .setParameter("next", Timestamp.valueOf(LocalDateTime.now().plusSeconds(delaySeconds)))
                .setParameter("id", event.getId())
                .setParameter("token", event.getClaimToken())
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return entityManager.createQuery(
                "SELECT COUNT(e) FROM AuthOutbox e WHERE e.status = 'PENDING'", Long.class)
                .getSingleResult();
    }
}
