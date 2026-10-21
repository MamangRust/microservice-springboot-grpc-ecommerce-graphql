package com.sanedge.ecommerce.merchant_social_link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.merchant_social_link.models.MerchantSocialMediaLink;

@Repository
public interface MerchantSocialMediaLinkRepository
        extends JpaRepository<MerchantSocialMediaLink, Long>,
        MerchantSocialMediaLinkRepositoryCustom {
}
