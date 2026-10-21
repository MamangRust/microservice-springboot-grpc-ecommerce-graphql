package com.sanedge.ecommerce.merchant_business.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_business.models.MerchantBusinessInformation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantBusinessCommandRepositoryImpl
        implements MerchantBusinessCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchant_business_information
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_business_info_id = :merchantBusinessInfoId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchant_business_information
            SET deleted_at = NULL
            WHERE merchant_business_info_id = :merchantBusinessInfoId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchant_business_information
            WHERE merchant_business_info_id = :merchantBusinessInfoId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchant_business_information
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchant_business_information
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public MerchantBusinessInformation trashed(Integer merchantBusinessInfoId) {
        MerchantBusinessInformation info = em.find(MerchantBusinessInformation.class,
                merchantBusinessInfoId.longValue());

        if (info != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantBusinessInfoId", merchantBusinessInfoId)
                    .executeUpdate();

            em.refresh(info);
        }

        return info;
    }

    @Override
    @Transactional
    public MerchantBusinessInformation restore(Integer merchantBusinessInfoId) {
        MerchantBusinessInformation info = em.find(MerchantBusinessInformation.class,
                merchantBusinessInfoId.longValue());

        if (info != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantBusinessInfoId", merchantBusinessInfoId)
                    .executeUpdate();

            em.refresh(info);
        }

        return info;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Integer merchantBusinessInfoId) {
        MerchantBusinessInformation info = em.find(MerchantBusinessInformation.class,
                merchantBusinessInfoId.longValue());

        if (info != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantBusinessInfoId", merchantBusinessInfoId)
                    .executeUpdate();

            em.detach(info);

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
