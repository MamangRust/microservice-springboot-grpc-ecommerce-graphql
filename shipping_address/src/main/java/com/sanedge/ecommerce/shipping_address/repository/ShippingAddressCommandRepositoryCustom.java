package com.sanedge.ecommerce.shipping_address.repository;

import com.sanedge.ecommerce.shipping_address.models.ShippingAddress;

public interface ShippingAddressCommandRepositoryCustom {
    ShippingAddress trashed(Long shippingAddressId);

    ShippingAddress restore(Long shippingAddressId);

    boolean deletePermanent(Long shippingAddressId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
