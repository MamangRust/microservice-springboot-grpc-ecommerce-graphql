package com.sanedge.ecommerce.order.repository;

import com.sanedge.ecommerce.order.models.Order;

public interface OrderCommandRepositoryCustom {
    Order trashed(Long orderId);

    Order restore(Long orderId);

    Order deletePermanent(Long orderId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
