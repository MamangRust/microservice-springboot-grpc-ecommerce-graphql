package com.sanedge.ecommerce.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.order.models.Order;

@Repository
public interface OrderCommandRepository extends
        JpaRepository<Order, Long>,
        OrderCommandRepositoryCustom {
}
