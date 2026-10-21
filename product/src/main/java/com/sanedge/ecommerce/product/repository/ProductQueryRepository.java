package com.sanedge.ecommerce.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.product.models.Product;

@Repository
public interface ProductQueryRepository extends JpaRepository<Product, Long> {
        @Query("""
                        SELECT p FROM Product p
                        WHERE p.deletedAt IS NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.slugProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<Product> findProducts(@Param("keyword") String keyword, Pageable pageable);

        @Query(value = """
                        WITH filtered_products AS (
                            SELECT
                                p.product_id,
                                p.merchant_id,
                                p.category_id,
                                p.weight,
                                p.rating,
                                p.slug_product,
                                p.name,
                                p.description,
                                p.price,
                                p.count_in_stock,
                                p.brand,
                                p.image_product,
                                p.created_at,
                                p.updated_at,
                                c.name AS category_name
                            FROM
                                products p
                            JOIN
                                categories c ON p.category_id = c.category_id
                            WHERE
                                p.deleted_at IS NULL
                                AND p.merchant_id = :merchantId
                                AND (
                                    :keyword IS NULL OR :keyword = '' OR
                                    p.name ILIKE CONCAT('%', :keyword, '%') OR
                                    p.description ILIKE CONCAT('%', :keyword, '%') OR
                                    p.slug_product ILIKE CONCAT('%', :keyword, '%')
                                )
                                AND (
                                    (:categoryId IS NULL OR :categoryId = 0) OR c.category_id = :categoryId
                                )
                                AND (
                                    p.price >= COALESCE(:minPrice, 0)
                                    AND p.price <= COALESCE(:maxPrice, 999999999)
                                )
                        )
                        SELECT
                            fp.product_id,
                            fp.merchant_id,
                            fp.category_id,
                            fp.weight,
                            fp.rating,
                            fp.slug_product,
                            fp.name,
                            fp.description,
                            fp.price,
                            fp.count_in_stock,
                            fp.brand,
                            fp.image_product,
                            fp.created_at,
                            fp.updated_at,
                            fp.category_name
                        FROM filtered_products fp
                        ORDER BY fp.created_at DESC
                        """, countQuery = """
                        SELECT COUNT(*) FROM products p
                        JOIN categories c ON p.category_id = c.category_id
                        WHERE
                            p.deleted_at IS NULL
                            AND p.merchant_id = :merchantId
                            AND (
                                :keyword IS NULL OR :keyword = '' OR
                                p.name ILIKE CONCAT('%', :keyword, '%') OR
                                p.description ILIKE CONCAT('%', :keyword, '%') OR
                                p.slug_product ILIKE CONCAT('%', :keyword, '%')
                            )
                            AND (
                                (:categoryId IS NULL OR :categoryId = 0) OR c.category_id = :categoryId
                            )
                            AND (
                                p.price >= COALESCE(:minPrice, 0)
                                AND p.price <= COALESCE(:maxPrice, 999999999)
                            )
                        """, nativeQuery = true)
        Page<Product> findProductsByMerchantNative(
                        @Param("merchantId") Integer merchantId,
                        @Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        @Param("minPrice") Integer minPrice,
                        @Param("maxPrice") Integer maxPrice,
                        Pageable pageable);

        @Query(value = """
                        WITH filtered_products AS (
                            SELECT
                                p.product_id,
                                p.merchant_id,
                                p.category_id,
                                p.weight,
                                p.rating,
                                p.slug_product,
                                p.name,
                                p.description,
                                p.price,
                                p.count_in_stock,
                                p.brand,
                                p.image_product,
                                p.created_at,
                                p.updated_at,
                                c.name AS category_name
                            FROM
                                products p
                            JOIN
                                categories c ON p.category_id = c.category_id
                            WHERE
                                p.deleted_at IS NULL
                                AND c.name = :categoryName
                                AND (
                                    :keyword IS NULL OR :keyword = '' OR
                                    p.name ILIKE CONCAT('%', :keyword, '%') OR
                                    p.description ILIKE CONCAT('%', :keyword, '%') OR
                                    p.slug_product ILIKE CONCAT('%', :keyword, '%')
                                )
                                AND (
                                    p.price >= COALESCE(:minPrice, 0)
                                    AND p.price <= COALESCE(:maxPrice, 999999999)
                                )
                        )
                        SELECT
                            fp.product_id,
                            fp.merchant_id,
                            fp.category_id,
                            fp.weight,
                            fp.rating,
                            fp.slug_product,
                            fp.name,
                            fp.description,
                            fp.price,
                            fp.count_in_stock,
                            fp.brand,
                            fp.image_product,
                            fp.created_at,
                            fp.updated_at,
                            fp.category_name
                        FROM filtered_products fp
                        ORDER BY fp.created_at DESC
                        """, countQuery = """
                        SELECT COUNT(*) FROM products p
                        JOIN categories c ON p.category_id = c.category_id
                        WHERE
                            p.deleted_at IS NULL
                            AND c.name = :categoryName
                            AND (
                                :keyword IS NULL OR :keyword = '' OR
                                p.name ILIKE CONCAT('%', :keyword, '%') OR
                                p.description ILIKE CONCAT('%', :keyword, '%') OR
                                p.slug_product ILIKE CONCAT('%', :keyword, '%')
                            )
                            AND (
                                p.price >= COALESCE(:minPrice, 0)
                                AND p.price <= COALESCE(:maxPrice, 999999999)
                            )
                        """, nativeQuery = true)
        Page<Product> findProductsByCategoryNameNative(
                        @Param("categoryName") String categoryName,
                        @Param("keyword") String keyword,
                        @Param("minPrice") Integer minPrice,
                        @Param("maxPrice") Integer maxPrice,
                        Pageable pageable);

        @Query("""
                        SELECT p FROM Product p
                        WHERE p.deletedAt IS NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.slugProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<Product> findActiveProducts(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT p FROM Product p
                        WHERE p.deletedAt IS NOT NULL
                        AND (
                            :keyword IS NULL OR
                            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.slugProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<Product> findTrashedProducts(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                        SELECT p FROM Product p
                        WHERE p.deletedAt IS NULL
                        AND p.merchantId = :merchantId
                        AND (
                            :keyword IS NULL OR
                            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.slugProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<Product> findByMerchant(
                        @Param("merchantId") Integer merchantId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT p FROM Product p
                        WHERE p.deletedAt IS NULL
                        AND p.categoryId = :categoryId
                        AND (
                            :keyword IS NULL OR
                            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                            LOWER(p.slugProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        """)
        Page<Product> findByCategory(
                        @Param("categoryId") Integer categoryId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query(value = "SELECT * FROM products WHERE product_id = :productId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
        Optional<Product> findProductById(@Param("productId") Long productId);
}
