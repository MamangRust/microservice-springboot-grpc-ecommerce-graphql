package com.sanedge.ecommerce.product.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.product.models.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class ProductCommandRepositoryImpl implements ProductCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE products
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE product_id = :productId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE products
            SET deleted_at = NULL
            WHERE product_id = :productId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM products
            WHERE product_id = :productId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE products
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM products
            WHERE deleted_at IS NOT NULL
            """;

    private static final String UPDATE_STOCK_QUERY = """
            UPDATE products
            SET count_in_stock = :count
            WHERE product_id = :productId
              AND deleted_at IS NULL
            """;

    @Override
    @Transactional
    public Product trashed(Long productId) {
        Product product = em.find(Product.class, productId);

        if (product != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("productId", productId)
                    .executeUpdate();

            em.refresh(product);
        }

        return product;
    }

    @Override
    @Transactional
    public Product restore(Long productId) {
        Product product = em.find(Product.class, productId);

        if (product != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("productId", productId)
                    .executeUpdate();

            em.refresh(product);
        }

        return product;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long productId) {
        Product product = em.find(Product.class, productId);

        if (product != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("productId", productId)
                    .executeUpdate();

            em.detach(product);

            return deleted > 0;
        }

        return false;
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
    public Product updateCountInStock(Long productId, Integer count) {
        Product product = em.find(Product.class, productId);

        if (product != null) {
            em.createNativeQuery(UPDATE_STOCK_QUERY)
                    .setParameter("productId", productId)
                    .setParameter("count", count)
                    .executeUpdate();

            em.refresh(product);
        }

        return product;
    }
}
