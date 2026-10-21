package com.sanedge.ecommerce.merchant_policy.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_policy.models.MerchantPolicy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantPolicyCommandRepositoryImpl implements MerchantPolicyCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchant_policies
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_policy_id = :merchantPolicyId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchant_policies
            SET deleted_at = NULL
            WHERE merchant_policy_id = :merchantPolicyId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchant_policies
            WHERE merchant_policy_id = :merchantPolicyId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchant_policies
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchant_policies
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public MerchantPolicy trashed(Long merchantPolicyId) {
        MerchantPolicy merchantPolicy = em.find(MerchantPolicy.class, merchantPolicyId);

        if (merchantPolicy != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantPolicyId", merchantPolicyId)
                    .executeUpdate();

            em.refresh(merchantPolicy);
        }

        return merchantPolicy;
    }

    @Override
    @Transactional
    public MerchantPolicy restore(Long merchantPolicyId) {
        MerchantPolicy merchantPolicy = em.find(MerchantPolicy.class, merchantPolicyId);

        if (merchantPolicy != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantPolicyId", merchantPolicyId)
                    .executeUpdate();

            em.refresh(merchantPolicy);
        }

        return merchantPolicy;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long merchantPolicyId) {
        MerchantPolicy merchantPolicy = em.find(MerchantPolicy.class, merchantPolicyId);

        if (merchantPolicy != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantPolicyId", merchantPolicyId)
                    .executeUpdate();

            em.detach(merchantPolicy);

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
