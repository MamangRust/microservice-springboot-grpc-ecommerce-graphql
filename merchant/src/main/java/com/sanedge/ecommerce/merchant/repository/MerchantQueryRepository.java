package com.sanedge.ecommerce.merchant.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant.models.Merchant;

@Repository
public interface MerchantQueryRepository extends JpaRepository<Merchant, Long> {
    @Query("""
            SELECT m FROM Merchant m
            WHERE m.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Merchant> findMerchants(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT m FROM Merchant m
            WHERE m.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Merchant> findActiveMerchants(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT m FROM Merchant m
            WHERE m.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.contactPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Merchant> findTrashedMerchants(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM merchants WHERE merchant_id = :merchantId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<Merchant> findMerchantById(@Param("merchantId") Long merchantId);

    @Query(value = "SELECT * FROM merchants WHERE user_id = :userId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<Merchant> findMerchantByUserId(@Param("userId") Integer userId);
}
