package com.sanedge.ecommerce.slider.repository;

import com.sanedge.ecommerce.slider.models.Slider;

public interface SliderCommandRepositoryCustom {
    Slider trashed(Long sliderId);

    Slider restore(Long sliderId);

    boolean deletePermanent(Long sliderId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
