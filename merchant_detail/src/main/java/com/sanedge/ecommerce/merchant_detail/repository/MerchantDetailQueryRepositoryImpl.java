package com.sanedge.ecommerce.merchant_detail.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.ecommerce.merchant_detail.models.MerchantDetailsRelation;
import com.sanedge.ecommerce.merchant_detail.models.MerchantSocialMediaLink;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class MerchantDetailQueryRepositoryImpl implements MerchantDetailQueryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<MerchantDetailsRelation> findAllWithSocialLinks(String keyword, Pageable pageable) {
        return queryWithSocialLinks(keyword, null, null, pageable);
    }

    @Override
    public Page<MerchantDetailsRelation> findActiveWithSocialLinks(String keyword, Pageable pageable) {
        return queryWithSocialLinks(keyword, true, null, pageable);
    }

    @Override
    public Page<MerchantDetailsRelation> findTrashedWithSocialLinks(String keyword, Pageable pageable) {
        return queryWithSocialLinks(keyword, false, null, pageable);
    }

    @Override
    public Optional<MerchantDetailsRelation> findByIdWithSocialLinks(Long merchantDetailId) {
        List<MerchantDetailsRelation> results = queryWithSocialLinks(null, null, merchantDetailId, Pageable.unpaged())
                .getContent();
        if (results.isEmpty())
            return Optional.empty();
        return Optional.of(results.get(0));
    }

    private Page<MerchantDetailsRelation> queryWithSocialLinks(String keyword, Boolean isActive, Long id,
            Pageable pageable) {
        String baseSql = "FROM merchant_details md " +
                "JOIN merchants m ON md.merchant_id = m.merchant_id " +
                "LEFT JOIN merchant_social_media_links sml ON sml.merchant_detail_id = md.merchant_detail_id " +
                "WHERE 1=1 ";

        if (keyword != null) {
            baseSql += "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ";
        }
        if (isActive != null) {
            baseSql += isActive ? "AND m.deleted_at IS NULL " : "AND m.deleted_at IS NOT NULL ";
        }
        if (id != null) {
            baseSql += "AND md.merchant_detail_id = :id ";
        }

        String countSql = "SELECT COUNT(DISTINCT md.merchant_detail_id) " + baseSql;
        Query countQuery = em.createNativeQuery(countSql);
        if (keyword != null)
            countQuery.setParameter("keyword", keyword);
        if (id != null)
            countQuery.setParameter("id", id);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = "SELECT " +
                "md.merchant_detail_id, md.merchant_id, md.display_name, md.cover_image_url, md.logo_url, " +
                "md.short_description, md.website_url, md.created_at, md.updated_at, md.deleted_at, " +
                "m.name AS merchant_name, " +
                "json_agg(json_build_object('id', sml.merchant_social_id, 'platform', sml.platform, 'url', sml.url)) AS social_media_links "
                +
                baseSql +
                "GROUP BY md.merchant_detail_id, m.merchant_id " +
                "ORDER BY md.created_at DESC " +
                (pageable.isPaged() ? "LIMIT :limit OFFSET :offset" : "");

        Query dataQuery = em.createNativeQuery(dataSql);
        if (keyword != null)
            dataQuery.setParameter("keyword", keyword);
        if (id != null)
            dataQuery.setParameter("id", id);
        if (pageable.isPaged()) {
            dataQuery.setParameter("limit", pageable.getPageSize());
            dataQuery.setParameter("offset", pageable.getOffset());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = dataQuery.getResultList();
        List<MerchantDetailsRelation> relations = mapResults(results);

        return new PageImpl<>(relations, pageable, total);
    }

    private List<MerchantDetailsRelation> mapResults(List<Object[]> results) {
        List<MerchantDetailsRelation> relations = new ArrayList<>();
        for (Object[] row : results) {
            MerchantDetailsRelation dto = new MerchantDetailsRelation();
            dto.setId(((Number) row[0]).intValue());
            dto.setMerchantId(((Number) row[1]).intValue());
            dto.setDisplayName((String) row[2]);
            dto.setCoverImageUrl((String) row[3]);
            dto.setLogoUrl((String) row[4]);
            dto.setShortDescription((String) row[5]);
            dto.setWebsiteUrl((String) row[6]);
            dto.setCreatedAt(row[7] != null ? row[7].toString() : null);
            dto.setUpdatedAt(row[8] != null ? row[8].toString() : null);
            dto.setDeletedAt(row[9] != null ? row[9].toString() : null);

            if (row[11] != null) {
                try {
                    List<MerchantSocialMediaLink> links = Arrays.asList(
                            new ObjectMapper().readValue(row[11].toString(), MerchantSocialMediaLink[].class));
                    dto.setSocialMediaLinks(links);
                } catch (Exception e) {
                    dto.setSocialMediaLinks(Collections.emptyList());
                }
            } else {
                dto.setSocialMediaLinks(Collections.emptyList());
            }
            relations.add(dto);
        }
        return relations;
    }
}
