package com.sanedge.ecommerce.slider.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.slider.models.Slider;

@Repository
public interface SliderCommandRepository extends JpaRepository<Slider, Long>, SliderCommandRepositoryCustom {
}
