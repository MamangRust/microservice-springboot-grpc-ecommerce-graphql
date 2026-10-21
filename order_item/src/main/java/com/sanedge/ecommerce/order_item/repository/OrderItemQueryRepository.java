package com.sanedge.ecommerce.order_item.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.order_item.models.OrderItem;

@Repository
public interface OrderItemQueryRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
                SELECT o FROM OrderItem o
                ORDER BY o.createdAt ASC
            """)
    Page<OrderItem> findOrderItems(Pageable pageable);

    @Query("""
                SELECT o FROM OrderItem o
                WHERE o.deletedAt IS NULL
                ORDER BY o.createdAt ASC
            """)
    Page<OrderItem> findActiveOrderItems(Pageable pageable);

    @Query("""
                SELECT o FROM OrderItem o
                WHERE o.deletedAt IS NOT NULL
                ORDER BY o.deletedAt DESC
            """)
    Page<OrderItem> findTrashedOrderItems(Pageable pageable);

    @Query(value = """
            SELECT * FROM order_items
            WHERE order_id = :orderId
              AND deleted_at IS NULL
            """, nativeQuery = true)
    List<OrderItem> findOrderItemByOrder(@Param("orderId") Long orderId);
}
