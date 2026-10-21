package com.sanedge.ecommerce.merchant_detail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_detail.models.MerchantDetail;

@Repository
public interface MerchantDetailCommandRepository
        extends JpaRepository<MerchantDetail, Long>, MerchantDetailCommandRepositoryCustom {

}
