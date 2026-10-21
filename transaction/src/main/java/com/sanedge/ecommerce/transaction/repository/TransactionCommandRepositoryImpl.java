package com.sanedge.ecommerce.transaction.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.transaction.models.Transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class TransactionCommandRepositoryImpl implements TransactionCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE transactions
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE transaction_id = :transactionId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE transactions
            SET deleted_at = NULL
            WHERE transaction_id = :transactionId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM transactions
            WHERE transaction_id = :transactionId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE transactions
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM transactions
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_BY_ORDER_PERMANENT_QUERY = """
            DELETE FROM transactions
            WHERE order_id = :orderId
              AND deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Transaction trashed(Long transactionId) {
        Transaction tx = em.find(Transaction.class, transactionId);

        if (tx != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("transactionId", transactionId)
                    .executeUpdate();

            em.refresh(tx);
        }

        return tx;
    }

    @Override
    @Transactional
    public Transaction restore(Long transactionId) {
        Transaction tx = em.find(Transaction.class, transactionId);

        if (tx != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("transactionId", transactionId)
                    .executeUpdate();

            em.refresh(tx);
        }

        return tx;
    }

    @Override
    @Transactional
    public Transaction deletePermanent(Long transactionId) {
        Transaction tx = em.find(Transaction.class, transactionId);

        if (tx != null) {
            em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("transactionId", transactionId)
                    .executeUpdate();

            em.detach(tx);
        }

        return tx;
    }

    @Override
    @Transactional
    public boolean restoreAllDeleted() {
        int updated = em.createNativeQuery(RESTORE_ALL_QUERY)
                .executeUpdate();

        return updated > 0;
    }

    @Override
    @Transactional
    public boolean deleteAllDeleted() {
        int deleted = em.createNativeQuery(DELETE_ALL_QUERY)
                .executeUpdate();

        return deleted > 0;
    }

    @Override
    @Transactional
    public boolean deleteByOrderPermanent(Integer orderId) {
        int deleted = em.createNativeQuery(DELETE_BY_ORDER_PERMANENT_QUERY)
                .setParameter("orderId", orderId)
                .executeUpdate();

        return deleted > 0;
    }
}
