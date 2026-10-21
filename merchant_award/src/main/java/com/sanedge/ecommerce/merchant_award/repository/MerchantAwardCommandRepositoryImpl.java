package com.sanedge.ecommerce.merchant_award.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_award.models.MerchantCertificationAndAward;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantAwardCommandRepositoryImpl
        implements MerchantAwardCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchant_certifications_and_awards
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_certification_id = :merchantCertificationId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchant_certifications_and_awards
            SET deleted_at = NULL
            WHERE merchant_certification_id = :merchantCertificationId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchant_certifications_and_awards
            WHERE merchant_certification_id = :merchantCertificationId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchant_certifications_and_awards
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchant_certifications_and_awards
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public MerchantCertificationAndAward trashed(Long merchantCertificationId) {
        MerchantCertificationAndAward award = em.find(MerchantCertificationAndAward.class, merchantCertificationId);

        if (award != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantCertificationId", merchantCertificationId)
                    .executeUpdate();

            em.refresh(award);
        }

        return award;
    }

    @Override
    @Transactional
    public MerchantCertificationAndAward restore(Long merchantCertificationId) {
        MerchantCertificationAndAward award = em.find(MerchantCertificationAndAward.class, merchantCertificationId);

        if (award != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantCertificationId", merchantCertificationId)
                    .executeUpdate();

            em.refresh(award);
        }

        return award;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long merchantCertificationId) {
        MerchantCertificationAndAward award = em.find(MerchantCertificationAndAward.class, merchantCertificationId);

        if (award != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantCertificationId", merchantCertificationId)
                    .executeUpdate();

            em.detach(award);

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
