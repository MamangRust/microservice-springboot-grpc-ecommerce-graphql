package com.sanedge.ecommerce.merchant.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant.models.Merchant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantCommandRepositoryImpl implements MerchantCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchants
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_id = :merchantId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchants
            SET deleted_at = NULL
            WHERE merchant_id = :merchantId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchants
            WHERE merchant_id = :merchantId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchants
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchants
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Merchant trashed(Long merchantId) {
        Merchant merchant = em.find(Merchant.class, merchantId);

        if (merchant != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantId", merchantId)
                    .executeUpdate();

            em.refresh(merchant);
        }

        return merchant;
    }

    @Override
    @Transactional
    public Merchant restore(Long merchantId) {
        Merchant merchant = em.find(Merchant.class, merchantId);

        if (merchant != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantId", merchantId)
                    .executeUpdate();

            em.refresh(merchant);
        }

        return merchant;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long merchantId) {
        Merchant merchant = em.find(Merchant.class, merchantId);

        if (merchant != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantId", merchantId)
                    .executeUpdate();

            em.detach(merchant);

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
