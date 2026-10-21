package com.sanedge.ecommerce.slider.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.slider.models.Slider;

@Repository
public interface SliderQueryRepository extends JpaRepository<Slider, Long> {
        @Query("""
                        SELECT s FROM Slider s
                        WHERE s.deletedAt IS NULL
                        AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<Slider> findSliders(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT s FROM Slider s
                        WHERE s.deletedAt IS NULL
                        AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<Slider> findActiveSliders(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT s FROM Slider s
                        WHERE s.deletedAt IS NOT NULL
                        AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<Slider> findTrashedSliders(@Param("keyword") String keyword, Pageable pageable);
}
