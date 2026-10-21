package com.sanedge.ecommerce.shipping_address.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.shipping_address.models.ShippingAddress;

@Repository
public interface ShippingAddressCommandRepository
                extends JpaRepository<ShippingAddress, Long>, ShippingAddressCommandRepositoryCustom {

}
