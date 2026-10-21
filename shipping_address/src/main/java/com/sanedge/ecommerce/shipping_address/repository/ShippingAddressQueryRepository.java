package com.sanedge.ecommerce.shipping_address.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.shipping_address.models.ShippingAddress;

@Repository
public interface ShippingAddressQueryRepository extends JpaRepository<ShippingAddress, Long> {
    @Query("""
            SELECT s FROM ShippingAddress s
            WHERE s.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(s.kota) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.provinsi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.negara) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<ShippingAddress> findShippingAddresses(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT s FROM ShippingAddress s
            WHERE s.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(s.kota) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.provinsi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.negara) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<ShippingAddress> findActiveShippingAddresses(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT s FROM ShippingAddress s
            WHERE s.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(s.kota) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.provinsi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(s.negara) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<ShippingAddress> findTrashedShippingAddresses(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM shipping_addresses WHERE shipping_address_id = :id AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<ShippingAddress> findByIdNative(@Param("id") Long id);

    @Query(value = "SELECT * FROM shipping_addresses WHERE order_id = :orderId AND deleted_at IS NULL", nativeQuery = true)
    Optional<ShippingAddress> findByOrderId(@Param("orderId") Integer orderId);
}
