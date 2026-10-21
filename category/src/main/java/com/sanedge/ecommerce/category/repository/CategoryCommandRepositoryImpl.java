package com.sanedge.ecommerce.category.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.category.models.Category;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class CategoryCommandRepositoryImpl implements CategoryCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE categories
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE category_id = :categoryId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE categories
            SET deleted_at = NULL
            WHERE category_id = :categoryId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM categories
            WHERE category_id = :categoryId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE categories
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM categories
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Category trashed(Long categoryId) {
        Category category = em.find(Category.class, categoryId);

        if (category != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("categoryId", categoryId)
                    .executeUpdate();

            em.refresh(category);
        }

        return category;
    }

    @Override
    @Transactional
    public Category restore(Long categoryId) {
        Category category = em.find(Category.class, categoryId);

        if (category != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("categoryId", categoryId)
                    .executeUpdate();

            em.refresh(category);
        }

        return category;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long categoryId) {
        Category category = em.find(Category.class, categoryId);

        if (category != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("categoryId", categoryId)
                    .executeUpdate();

            em.detach(category);

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
}
