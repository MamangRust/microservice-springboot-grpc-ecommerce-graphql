package com.sanedge.ecommerce.cart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.cart.models.Cart;

@Repository
public interface CartQueryRepository extends JpaRepository<Cart, Long> {

    @Query("""
            SELECT c FROM Cart c
            WHERE c.deletedAt IS NULL
              AND c.userId = :userId
              AND (
                :keyword IS NULL OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                CAST(c.price AS string) LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Cart> findCartsByUser(
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
