package com.sanedge.ecommerce.banner.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.banner.models.Banner;

@Repository
public interface BannerQueryRepository extends JpaRepository<Banner, Long> {
    @Query("""
            SELECT b FROM Banner b
            WHERE b.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Banner> findBanners(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.deletedAt IS NULL
            AND b.isActive = true
            AND (
                :keyword IS NULL OR
                LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Banner> findActiveBanners(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Banner> findTrashedBanners(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Banner b
            WHERE b.deletedAt IS NULL
            AND LOWER(b.name) = LOWER(:name)
            """)
    Optional<Banner> findByName(@Param("name") String name);
}
