package com.sanedge.ecommerce.banner.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.banner.models.Banner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class BannerCommandRepositoryImpl implements BannerCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE banners
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE banner_id = :bannerId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE banners
            SET deleted_at = NULL
            WHERE banner_id = :bannerId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM banners
            WHERE banner_id = :bannerId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE banners
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM banners
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Banner trashed(Long bannerId) {
        Banner banner = em.find(Banner.class, bannerId);

        if (banner != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("bannerId", bannerId)
                    .executeUpdate();

            em.refresh(banner);
        }

        return banner;
    }

    @Override
    @Transactional
    public Banner restore(Long bannerId) {
        Banner banner = em.find(Banner.class, bannerId);

        if (banner != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("bannerId", bannerId)
                    .executeUpdate();

            em.refresh(banner);
        }

        return banner;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long bannerId) {
        Banner banner = em.find(Banner.class, bannerId);

        if (banner != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("bannerId", bannerId)
                    .executeUpdate();

            em.detach(banner);

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
