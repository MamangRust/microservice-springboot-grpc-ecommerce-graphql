package com.sanedge.ecommerce.transaction.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;
import com.sanedge.ecommerce.transaction.models.TransactionOutbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class TransactionOutboxRepository implements OutboxEventRepository<TransactionOutbox> {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${transaction.outbox.lease.seconds:300}")
    private long leaseSeconds;

    /** Persists a new pending event; call inside the business transaction. */
    @Transactional
    public void enqueue(TransactionOutbox event) {
        entityManager.persist(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionOutbox> findDue(int limit) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        return entityManager.createQuery(
                "SELECT e FROM TransactionOutbox e "
                        + "WHERE e.status = 'PENDING' "
                        + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now) "
                        + "AND (e.claimedAt IS NULL OR e.claimedAt < :leaseCutoff) "
                        + "ORDER BY e.createdAt",
                TransactionOutbox.class)
                .setParameter("now", now)
                .setParameter("leaseCutoff", leaseCutoff)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional
    public TransactionOutbox claim(TransactionOutbox event) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        String claimToken = UUID.randomUUID().toString();

        int updated = entityManager.createQuery(
                "UPDATE TransactionOutbox e "
                        + "SET e.claimedAt = :now, e.claimToken = :token, e.attempts = e.attempts + 1 "
                        + "WHERE e.id = :id AND e.status = 'PENDING' "
                        + "AND (e.claimedAt IS NULL OR e.claimedAt < :leaseCutoff) "
                        + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)")
                .setParameter("now", now)
                .setParameter("token", claimToken)
                .setParameter("id", event.getId())
                .setParameter("leaseCutoff", leaseCutoff)
                .executeUpdate();

        return updated > 0 ? entityManager.find(TransactionOutbox.class, event.getId()) : null;
    }

    @Override
    @Transactional
    public void markSent(TransactionOutbox event) {
        entityManager.createQuery(
                "UPDATE TransactionOutbox e SET e.status = 'SENT', e.claimedAt = NULL, e.lastError = NULL "
                        + "WHERE e.id = :id AND e.status = 'PENDING' AND e.claimToken = :token")
                .setParameter("id", event.getId())
                .setParameter("token", event.getClaimToken())
                .executeUpdate();
    }

    @Override
    @Transactional
    public void markRetry(TransactionOutbox event, Throwable failure, long delaySeconds) {
        entityManager.createQuery(
                "UPDATE TransactionOutbox e SET e.status = 'PENDING', e.claimedAt = NULL, "
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
                "SELECT COUNT(e) FROM TransactionOutbox e WHERE e.status = 'PENDING'", Long.class)
                .getSingleResult();
    }
}
