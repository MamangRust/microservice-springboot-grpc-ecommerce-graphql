package com.sanedge.ecommerce.merchant_social_link.repository;

import java.util.Optional;

import com.sanedge.ecommerce.merchant_social_link.models.MerchantSocialMediaLink;

public interface MerchantSocialMediaLinkRepositoryCustom {
    MerchantSocialMediaLink trashed(Long merchantSocialId);

    MerchantSocialMediaLink restore(Long merchantSocialId);

    boolean deletePermanent(Long merchantSocialId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();

    Optional<MerchantSocialMediaLink> findByMerchantDetailIdAndPlatform(Integer merchantDetailId,
            String platform);
}
