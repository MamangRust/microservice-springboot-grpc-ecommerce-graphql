package com.sanedge.ecommerce.merchant_award.repository;

import com.sanedge.ecommerce.merchant_award.models.MerchantCertificationAndAward;

public interface MerchantAwardCommandRepositoryCustom {
    MerchantCertificationAndAward trashed(Long merchantCertificationId);

    MerchantCertificationAndAward restore(Long merchantCertificationId);

    boolean deletePermanent(Long merchantCertificationId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
