package com.sanedge.ecommerce.merchant_award.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_award.models.MerchantCertificationAndAward;

@Repository
public interface MerchantAwardCommandRepository
        extends JpaRepository<MerchantCertificationAndAward, Long>,
        MerchantAwardCommandRepositoryCustom {
}
