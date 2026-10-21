package com.sanedge.ecommerce.product.repository;

import com.sanedge.ecommerce.product.models.Product;

public interface ProductCommandRepositoryCustom {
    Product trashed(Long productId);

    Product restore(Long productId);

    boolean deletePermanent(Long productId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();

    Product updateCountInStock(Long productId, Integer count);
}
