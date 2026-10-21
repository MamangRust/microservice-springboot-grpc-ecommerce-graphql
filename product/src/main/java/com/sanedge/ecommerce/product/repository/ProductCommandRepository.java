package com.sanedge.ecommerce.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.product.models.Product;

@Repository
public interface ProductCommandRepository extends JpaRepository<Product, Long>, ProductCommandRepositoryCustom {
}
