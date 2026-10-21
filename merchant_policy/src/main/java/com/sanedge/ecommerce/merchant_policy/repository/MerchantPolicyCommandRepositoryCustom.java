package com.sanedge.ecommerce.merchant_policy.repository;

import com.sanedge.ecommerce.merchant_policy.models.MerchantPolicy;

public interface MerchantPolicyCommandRepositoryCustom {
    MerchantPolicy trashed(Long merchantPolicyId);

    MerchantPolicy restore(Long merchantPolicyId);

    boolean deletePermanent(Long merchantPolicyId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
