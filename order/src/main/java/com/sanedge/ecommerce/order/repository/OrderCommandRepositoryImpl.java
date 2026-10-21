package com.sanedge.ecommerce.order.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.order.models.Order;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class OrderCommandRepositoryImpl implements OrderCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE orders
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE order_id = :orderId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE orders
            SET deleted_at = NULL
            WHERE order_id = :orderId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM orders
            WHERE order_id = :orderId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE orders
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM orders
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Order trashed(Long orderId) {
        Order order = em.find(Order.class, orderId);

        if (order != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("orderId", orderId)
                    .executeUpdate();

            em.refresh(order);
        }

        return order;
    }

    @Override
    @Transactional
    public Order restore(Long orderId) {
        Order order = em.find(Order.class, orderId);

        if (order != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("orderId", orderId)
                    .executeUpdate();

            em.refresh(order);
        }

        return order;
    }

    @Override
    @Transactional
    public Order deletePermanent(Long orderId) {
        Order order = em.find(Order.class, orderId);

        if (order != null) {
            em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("orderId", orderId)
                    .executeUpdate();

            em.detach(order);
        }

        return order;
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
}
