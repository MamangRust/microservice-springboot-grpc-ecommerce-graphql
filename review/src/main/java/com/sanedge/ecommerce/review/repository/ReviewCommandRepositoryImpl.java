package com.sanedge.ecommerce.review.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.review.models.Review;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class ReviewCommandRepositoryImpl implements ReviewCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE reviews
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE review_id = :reviewId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE reviews
            SET deleted_at = NULL
            WHERE review_id = :reviewId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM reviews
            WHERE review_id = :reviewId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE reviews
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM reviews
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Review trashed(Long reviewId) {
        Review review = em.find(Review.class, reviewId);

        if (review != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("reviewId", reviewId)
                    .executeUpdate();

            em.refresh(review);
        }

        return review;
    }

    @Override
    @Transactional
    public Review restore(Long reviewId) {
        Review review = em.find(Review.class, reviewId);

        if (review != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("reviewId", reviewId)
                    .executeUpdate();

            em.refresh(review);
        }

        return review;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long reviewId) {
        Review review = em.find(Review.class, reviewId);

        if (review != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("reviewId", reviewId)
                    .executeUpdate();

            em.detach(review);

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
