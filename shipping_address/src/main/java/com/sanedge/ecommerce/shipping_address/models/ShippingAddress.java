package com.sanedge.ecommerce.shipping_address.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sanedge.ecommerce.common.models.BaseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shipping_addresses")
public class ShippingAddress extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_address_id")
    private Long shippingAddressId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String alamat;

    @Column(nullable = false, length = 255)
    private String provinsi;

    @Column(nullable = false, length = 255)
    private String negara;

    @Column(nullable = false, length = 255)
    private String kota;

    @Column(nullable = false, length = 100)
    private String courier;

    @Column(name = "shipping_method", nullable = false, length = 255)
    private String shippingMethod;

    @Column(name = "shipping_cost", nullable = false)
    private Integer shippingCost;
}
