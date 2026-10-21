package com.sanedge.ecommerce.merchant_social_link.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_social_link.models.MerchantSocialMediaLink;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class MerchantSocialMediaLinkRepositoryImpl implements MerchantSocialMediaLinkRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE merchant_social_media_links
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE merchant_social_id = :merchantSocialId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE merchant_social_media_links
            SET deleted_at = NULL
            WHERE merchant_social_id = :merchantSocialId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM merchant_social_media_links
            WHERE merchant_social_id = :merchantSocialId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE merchant_social_media_links
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM merchant_social_media_links
            WHERE deleted_at IS NOT NULL
            """;

    private static final String FIND_BY_PLATFORM_QUERY = """
            SELECT * FROM merchant_social_media_links
            WHERE merchant_detail_id = :merchantDetailId
              AND platform = :platform
              AND deleted_at IS NULL
            """;

    @Override
    @Transactional
    public MerchantSocialMediaLink trashed(Long merchantSocialId) {
        MerchantSocialMediaLink socialLink = em.find(MerchantSocialMediaLink.class, merchantSocialId);

        if (socialLink != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("merchantSocialId", merchantSocialId)
                    .executeUpdate();

            em.refresh(socialLink);
        }

        return socialLink;
    }

    @Override
    @Transactional
    public MerchantSocialMediaLink restore(Long merchantSocialId) {
        MerchantSocialMediaLink socialLink = em.find(MerchantSocialMediaLink.class, merchantSocialId);

        if (socialLink != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("merchantSocialId", merchantSocialId)
                    .executeUpdate();

            em.refresh(socialLink);
        }

        return socialLink;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long merchantSocialId) {
        MerchantSocialMediaLink socialLink = em.find(MerchantSocialMediaLink.class, merchantSocialId);

        if (socialLink != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("merchantSocialId", merchantSocialId)
                    .executeUpdate();

            em.detach(socialLink);

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
    public Optional<MerchantSocialMediaLink> findByMerchantDetailIdAndPlatform(Integer merchantDetailId,
            String platform) {
        try {
            MerchantSocialMediaLink result = (MerchantSocialMediaLink) em.createNativeQuery(FIND_BY_PLATFORM_QUERY,
                    MerchantSocialMediaLink.class)
                    .setParameter("merchantDetailId", merchantDetailId)
                    .setParameter("platform", platform)
                    .getSingleResult();

            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
