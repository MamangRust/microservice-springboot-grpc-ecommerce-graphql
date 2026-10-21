package com.sanedge.ecommerce.review_detail.repository.review_detail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.review_detail.models.review.ReviewDetail;

@Repository
public interface ReviewDetailRepository
        extends JpaRepository<ReviewDetail, Long>, ReviewDetailRepositoryCustom {

    @Query("""
                SELECT rd FROM ReviewDetail rd
                WHERE (:keyword IS NULL
                    OR LOWER(rd.caption) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(rd.url) LIKE LOWER(CONCAT('%', :keyword, '%')))
                ORDER BY rd.createdAt ASC
            """)
    Page<ReviewDetail> findDetails(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
                SELECT rd FROM ReviewDetail rd
                WHERE rd.deletedAt IS NULL
                AND (:keyword IS NULL
                    OR LOWER(rd.caption) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(rd.url) LIKE LOWER(CONCAT('%', :keyword, '%')))
                ORDER BY rd.createdAt ASC
            """)
    Page<ReviewDetail> findActiveDetails(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
                SELECT rd FROM ReviewDetail rd
                WHERE rd.deletedAt IS NOT NULL
                AND (:keyword IS NULL
                    OR LOWER(rd.caption) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(rd.url) LIKE LOWER(CONCAT('%', :keyword, '%')))
                ORDER BY rd.deletedAt DESC
            """)
    Page<ReviewDetail> findTrashedDetails(@Param("keyword") String keyword, Pageable pageable);
}
