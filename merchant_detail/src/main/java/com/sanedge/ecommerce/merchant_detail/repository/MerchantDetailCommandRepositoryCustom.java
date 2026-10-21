package com.sanedge.ecommerce.merchant_detail.repository;

import com.sanedge.ecommerce.merchant_detail.models.MerchantDetail;

public interface MerchantDetailCommandRepositoryCustom {
    MerchantDetail trashed(Long merchantDetailId);

    MerchantDetail restore(Long merchantDetailId);

    boolean deletePermanent(Long merchantDetailId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
