package com.sanedge.ecommerce.category.repository;

import com.sanedge.ecommerce.category.models.Category;

public interface CategoryCommandRepositoryCustom {
    Category trashed(Long categoryId);

    Category restore(Long categoryId);

    boolean deletePermanent(Long categoryId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
