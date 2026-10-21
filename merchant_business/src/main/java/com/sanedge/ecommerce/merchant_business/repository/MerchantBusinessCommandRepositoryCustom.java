package com.sanedge.ecommerce.merchant_business.repository;

import com.sanedge.ecommerce.merchant_business.models.MerchantBusinessInformation;

public interface MerchantBusinessCommandRepositoryCustom {
    MerchantBusinessInformation trashed(Integer merchantBusinessInfoId);

    MerchantBusinessInformation restore(Integer merchantBusinessInfoId);

    boolean deletePermanent(Integer merchantBusinessInfoId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
