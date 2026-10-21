package com.sanedge.ecommerce.merchant_policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_policy.models.MerchantPolicy;

@Repository
public interface MerchantPolicyCommandRepository
                extends JpaRepository<MerchantPolicy, Long>, MerchantPolicyCommandRepositoryCustom {

}
