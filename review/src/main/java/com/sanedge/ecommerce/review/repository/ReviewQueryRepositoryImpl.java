package com.sanedge.ecommerce.review.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.ecommerce.review.models.ReviewDetail;
import com.sanedge.ecommerce.review.models.ReviewRelationsDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class ReviewQueryRepositoryImpl implements ReviewQueryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Page<ReviewRelationsDetail> findByMerchantId(Integer merchantId, Integer rating, String search,
            Pageable pageable) {
        String baseSql = """
                FROM reviews r
                JOIN products p ON r.product_id = p.product_id
                WHERE r.deleted_at IS NULL
                  AND p.merchant_id = :merchantId
                  AND (:rating IS NULL OR r.rating = :rating)
                  AND (
                        :search IS NULL
                        OR r.name ILIKE CONCAT('%', :search, '%')
                        OR r.comment ILIKE CONCAT('%', :search, '%')
                      )
                """;

        String countSql = "SELECT COUNT(*) " + baseSql;
        Query countQuery = em.createNativeQuery(countSql);
        countQuery.setParameter("merchantId", merchantId);
        countQuery.setParameter("rating", rating);
        countQuery.setParameter("search", search == null || search.isBlank() ? null : search);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = """
                SELECT
                    r.review_id,
                    r.user_id,
                    r.product_id,
                    r.name,
                    r.comment,
                    r.rating,
                    r.created_at,
                    r.updated_at,
                    r.deleted_at,
                    COALESCE(
                        (SELECT json_agg(
                            jsonb_build_object(
                                'detail_id', rd.detail_id,
                                'type', rd.type,
                                'url', rd.url,
                                'caption', rd.caption,
                                'created_at', rd.created_at
                            )
                        )
                        FROM review_details rd
                        WHERE rd.review_id = r.review_id),
                        '[]'
                    ) AS review_details
                """ + baseSql + """
                ORDER BY r.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        Query dataQuery = em.createNativeQuery(dataSql);
        dataQuery.setParameter("merchantId", merchantId);
        dataQuery.setParameter("rating", rating);
        dataQuery.setParameter("search", search == null || search.isBlank() ? null : search);
        dataQuery.setParameter("limit", pageable.getPageSize());
        dataQuery.setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Object[]> results = dataQuery.getResultList();

        List<ReviewRelationsDetail> dtoList = mapResults(results);

        return new PageImpl<>(dtoList, pageable, total);
    }

    @Override
    public Page<ReviewRelationsDetail> findByProductId(Integer productId, Integer rating, String search,
            Pageable pageable) {
        String baseSql = """
                FROM reviews r
                WHERE r.deleted_at IS NULL
                  AND r.product_id = :productId
                  AND (:rating IS NULL OR r.rating = :rating)
                  AND (
                        :search IS NULL
                        OR r.name ILIKE CONCAT('%', :search, '%')
                        OR r.comment ILIKE CONCAT('%', :search, '%')
                      )
                """;

        String countSql = "SELECT COUNT(*) " + baseSql;
        Query countQuery = em.createNativeQuery(countSql);
        countQuery.setParameter("productId", productId);
        countQuery.setParameter("rating", rating);
        countQuery.setParameter("search", search == null || search.isBlank() ? null : search);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = """
                SELECT
                    r.review_id,
                    r.user_id,
                    r.product_id,
                    r.name,
                    r.comment,
                    r.rating,
                    r.created_at,
                    r.updated_at,
                    r.deleted_at,
                    COALESCE(
                        (SELECT json_agg(
                            jsonb_build_object(
                                'detail_id', rd.detail_id,
                                'type', rd.type,
                                'url', rd.url,
                                'caption', rd.caption,
                                'created_at', rd.created_at
                            )
                        )
                        FROM review_details rd
                        WHERE rd.review_id = r.review_id),
                        '[]'
                    ) AS review_details
                """ + baseSql + """
                ORDER BY r.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        Query dataQuery = em.createNativeQuery(dataSql);
        dataQuery.setParameter("productId", productId);
        dataQuery.setParameter("rating", rating);
        dataQuery.setParameter("search", search == null || search.isBlank() ? null : search);
        dataQuery.setParameter("limit", pageable.getPageSize());
        dataQuery.setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Object[]> results = dataQuery.getResultList();

        List<ReviewRelationsDetail> dtoList = mapResults(results);

        return new PageImpl<>(dtoList, pageable, total);
    }

    private List<ReviewRelationsDetail> mapResults(List<Object[]> results) {
        List<ReviewRelationsDetail> list = new ArrayList<>();

        for (Object[] row : results) {
            ReviewRelationsDetail dto = new ReviewRelationsDetail();
            dto.setId(((Number) row[0]).intValue());
            dto.setUserId(((Number) row[1]).intValue());
            dto.setProductId(((Number) row[2]).intValue());
            dto.setName((String) row[3]);
            dto.setComment((String) row[4]);
            dto.setRating(((Number) row[5]).intValue());
            dto.setCreatedAt(row[6] != null ? row[6].toString() : null);
            dto.setUpdatedAt(row[7] != null ? row[7].toString() : null);
            dto.setDeletedAt(row[8] != null ? row[8].toString() : null);

            if (row[9] != null) {
                try {
                    ReviewDetail[] detailsArray = mapper.readValue(row[9].toString(), ReviewDetail[].class);

                    List<ReviewDetail> detailsList = Arrays.stream(detailsArray)
                            .map(d -> new ReviewDetail(
                                    d.getReviewDetailId(),
                                    d.getReviewId(),
                                    d.getType(),
                                    d.getUrl(),
                                    d.getCaption()))
                            .toList();

                    dto.setReviewDetail(detailsList);

                } catch (Exception e) {
                    dto.setReviewDetail(Collections.emptyList());
                }
            } else {
                dto.setReviewDetail(Collections.emptyList());
            }

            list.add(dto);
        }

        return list;
    }

}
