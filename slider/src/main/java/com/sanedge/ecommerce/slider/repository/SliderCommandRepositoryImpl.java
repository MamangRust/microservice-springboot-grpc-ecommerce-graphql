package com.sanedge.ecommerce.slider.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.slider.models.Slider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class SliderCommandRepositoryImpl implements SliderCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE sliders
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE slider_id = :sliderId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE sliders
            SET deleted_at = NULL
            WHERE slider_id = :sliderId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM sliders
            WHERE slider_id = :sliderId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE sliders
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM sliders
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Slider trashed(Long sliderId) {
        Slider slider = em.find(Slider.class, sliderId);

        if (slider != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("sliderId", sliderId)
                    .executeUpdate();

            em.refresh(slider);
        }

        return slider;
    }

    @Override
    @Transactional
    public Slider restore(Long sliderId) {
        Slider slider = em.find(Slider.class, sliderId);

        if (slider != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("sliderId", sliderId)
                    .executeUpdate();

            em.refresh(slider);
        }

        return slider;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long sliderId) {
        Slider slider = em.find(Slider.class, sliderId);

        if (slider != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("sliderId", sliderId)
                    .executeUpdate();

            em.detach(slider);

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
