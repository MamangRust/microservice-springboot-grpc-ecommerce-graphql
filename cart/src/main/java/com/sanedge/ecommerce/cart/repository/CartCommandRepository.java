package com.sanedge.ecommerce.cart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.cart.models.Cart;

import jakarta.transaction.Transactional;

@Repository
public interface CartCommandRepository extends JpaRepository<Cart, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.cartId = :cartId")
    void deleteCartById(@Param("cartId") Long cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.cartId IN :cartIds")
    void deleteCartsByIds(@Param("cartIds") List<Long> cartIds);
}
