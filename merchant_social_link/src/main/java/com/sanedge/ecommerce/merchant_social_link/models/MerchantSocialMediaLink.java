package com.sanedge.ecommerce.merchant_social_link.models;

import com.sanedge.ecommerce.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "merchant_social_media_links")
public class MerchantSocialMediaLink extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_social_id")
    private Long merchantSocialId;

    @Column(name = "merchant_detail_id", nullable = false)
    private Integer merchantDetailId;

    @Column(nullable = false, length = 100)
    private String platform;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
}
