package com.sanedge.ecommerce.order_item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.order_item.models.OrderItem;

@Repository
public interface OrderItemCommandRepository extends JpaRepository<OrderItem, Long>, OrderItemCommandRepositoryCustom {
}
