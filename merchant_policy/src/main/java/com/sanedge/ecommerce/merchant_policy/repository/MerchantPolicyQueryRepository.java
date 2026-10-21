package com.sanedge.ecommerce.merchant_policy.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_policy.models.MerchantPolicy;

@Repository
public interface MerchantPolicyQueryRepository extends JpaRepository<MerchantPolicy, Long> {
    @Query("""
            SELECT mp FROM MerchantPolicy mp
            WHERE mp.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(mp.policyType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantPolicy> findMerchantPolicies(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT mp FROM MerchantPolicy mp
            WHERE mp.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(mp.policyType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantPolicy> findActiveMerchantPolicies(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT mp FROM MerchantPolicy mp
            WHERE mp.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(mp.policyType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(mp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<MerchantPolicy> findTrashedMerchantPolicies(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM merchant_policies WHERE merchant_policy_id = :merchantPolicyId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<MerchantPolicy> findMerchantPolicyById(@Param("merchantPolicyId") Long merchantPolicyId);
}
