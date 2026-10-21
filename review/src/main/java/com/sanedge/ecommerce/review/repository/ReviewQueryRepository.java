package com.sanedge.ecommerce.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.review.models.Review;

@Repository
public interface ReviewQueryRepository extends JpaRepository<Review, Long>, ReviewQueryRepositoryCustom {
    @Query("""
            SELECT r FROM Review r
            WHERE r.deletedAt IS NULL
            AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Review> findReviews(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.deletedAt IS NULL
            AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Review> findActiveReviews(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.deletedAt IS NOT NULL
            AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Review> findTrashedReviews(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM reviews WHERE review_id = :reviewId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<Review> findReviewById(@Param("reviewId") Long reviewId);
}
