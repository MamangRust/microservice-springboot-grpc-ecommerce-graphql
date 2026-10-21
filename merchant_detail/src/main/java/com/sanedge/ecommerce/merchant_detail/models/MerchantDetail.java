package com.sanedge.ecommerce.merchant_detail.models;

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
@Table(name = "merchant_details")
public class MerchantDetail extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_detail_id")
    private Long merchantDetailId;

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;
}
