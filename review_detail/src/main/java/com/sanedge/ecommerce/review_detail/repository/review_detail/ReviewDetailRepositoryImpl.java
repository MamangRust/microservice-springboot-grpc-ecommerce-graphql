package com.sanedge.ecommerce.review_detail.repository.review_detail;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.review_detail.models.review.ReviewDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class ReviewDetailRepositoryImpl implements ReviewDetailRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE review_details
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE review_detail_id = :reviewDetailId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE review_details
            SET deleted_at = NULL
            WHERE review_detail_id = :reviewDetailId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM review_details
            WHERE review_detail_id = :reviewDetailId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE review_details
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM review_details
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public ReviewDetail trashed(Long reviewDetailId) {
        ReviewDetail detail = em.find(ReviewDetail.class, reviewDetailId);

        if (detail != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("reviewDetailId", reviewDetailId)
                    .executeUpdate();

            em.refresh(detail);
        }

        return detail;
    }

    @Override
    @Transactional
    public ReviewDetail restore(Long reviewDetailId) {
        ReviewDetail detail = em.find(ReviewDetail.class, reviewDetailId);

        if (detail != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("reviewDetailId", reviewDetailId)
                    .executeUpdate();

            em.refresh(detail);
        }

        return detail;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long reviewDetailId) {
        ReviewDetail detail = em.find(ReviewDetail.class, reviewDetailId);

        if (detail != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("reviewDetailId", reviewDetailId)
                    .executeUpdate();

            em.detach(detail);

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
