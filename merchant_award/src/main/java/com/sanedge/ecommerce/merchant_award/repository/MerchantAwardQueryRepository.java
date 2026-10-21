package com.sanedge.ecommerce.merchant_award.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_award.models.MerchantCertificationAndAward;

@Repository
public interface MerchantAwardQueryRepository
                extends JpaRepository<MerchantCertificationAndAward, Long> {
        @Query("""
                        SELECT m FROM MerchantCertificationAndAward m
                        WHERE m.deletedAt IS NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.issuedBy) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.certificateUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<MerchantCertificationAndAward> findMerchantAwards(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT m FROM MerchantCertificationAndAward m
                        WHERE m.deletedAt IS NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.issuedBy) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.certificateUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<MerchantCertificationAndAward> findActiveMerchantAwards(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT m FROM MerchantCertificationAndAward m
                        WHERE m.deletedAt IS NOT NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.issuedBy) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(m.certificateUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<MerchantCertificationAndAward> findTrashedMerchantAwards(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query(value = """
                        SELECT * FROM merchant_certifications_and_awards
                        WHERE merchant_certification_id = :merchantCertificationId
                        AND deleted_at IS NULL
                        LIMIT 1
                        """, nativeQuery = true)
        Optional<MerchantCertificationAndAward> findMerchantAwardById(
                        @Param("merchantCertificationId") Long merchantCertificationId);
}
