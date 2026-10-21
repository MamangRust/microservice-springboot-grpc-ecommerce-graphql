package com.sanedge.ecommerce.merchant_business.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_business.models.MerchantBusinessInformation;

@Repository
public interface MerchantBusinessQueryRepository
        extends JpaRepository<MerchantBusinessInformation, Integer> {
    @Query("""
            SELECT m FROM MerchantBusinessInformation m
            WHERE m.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.businessType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.taxId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.websiteUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantBusinessInformation> findMerchantBusinessInformation(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT m FROM MerchantBusinessInformation m
            WHERE m.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.businessType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.taxId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.websiteUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantBusinessInformation> findActiveMerchantBusinessInformation(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT m FROM MerchantBusinessInformation m
            WHERE m.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.businessType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.taxId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.websiteUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantBusinessInformation> findTrashedMerchantBusinessInformation(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM merchant_business_information
            WHERE merchant_business_info_id = :merchantBusinessInfoId
            AND deleted_at IS NULL
            LIMIT 1
            """, nativeQuery = true)
    Optional<MerchantBusinessInformation> findMerchantBusinessInformationById(
            @Param("merchantBusinessInfoId") Integer merchantBusinessInfoId);
}
