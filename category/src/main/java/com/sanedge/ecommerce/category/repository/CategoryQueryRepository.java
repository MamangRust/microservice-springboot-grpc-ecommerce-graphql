package com.sanedge.ecommerce.category.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.category.models.Category;

@Repository
public interface CategoryQueryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT c FROM Category c
            WHERE c.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.slugCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Category> findCategories(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT c FROM Category c
            WHERE c.deletedAt IS NULL
            AND (
                :keyword IS NULL OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.slugCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Category> findActiveCategories(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT c FROM Category c
            WHERE c.deletedAt IS NOT NULL
            AND (
                :keyword IS NULL OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.slugCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Category> findTrashedCategories(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM categories WHERE category_id = :categoryId AND deleted_at IS NULL LIMIT 1", nativeQuery = true)
    Optional<Category> findCategoryById(@Param("categoryId") Long categoryId);
}
