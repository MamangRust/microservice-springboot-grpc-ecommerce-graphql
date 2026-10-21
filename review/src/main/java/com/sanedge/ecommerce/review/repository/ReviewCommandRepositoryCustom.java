package com.sanedge.ecommerce.review.repository;

import com.sanedge.ecommerce.review.models.Review;

public interface ReviewCommandRepositoryCustom {
    Review trashed(Long reviewId);

    Review restore(Long reviewId);

    boolean deletePermanent(Long reviewId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
