package com.sanedge.ecommerce.merchant_detail.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_detail.models.MerchantDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantDetailCommandRepositoryImpl implements MerchantDetailCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchant_details
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_detail_id = :merchantDetailId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchant_details
            SET deleted_at = NULL
            WHERE merchant_detail_id = :merchantDetailId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchant_details
            WHERE merchant_detail_id = :merchantDetailId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchant_details
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchant_details
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public MerchantDetail trashed(Long merchantDetailId) {
        MerchantDetail detail = em.find(MerchantDetail.class, merchantDetailId);

        if (detail != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantDetailId", merchantDetailId)
                    .executeUpdate();

            em.refresh(detail);
        }

        return detail;
    }

    @Override
    @Transactional
    public MerchantDetail restore(Long merchantDetailId) {
        MerchantDetail detail = em.find(MerchantDetail.class, merchantDetailId);

        if (detail != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantDetailId", merchantDetailId)
                    .executeUpdate();

            em.refresh(detail);
        }

        return detail;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long merchantDetailId) {
        MerchantDetail detail = em.find(MerchantDetail.class, merchantDetailId);

        if (detail != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantDetailId", merchantDetailId)
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
