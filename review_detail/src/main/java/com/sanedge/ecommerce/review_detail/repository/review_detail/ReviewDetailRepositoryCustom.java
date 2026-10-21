package com.sanedge.ecommerce.review_detail.repository.review_detail;

import com.sanedge.ecommerce.review_detail.models.review.ReviewDetail;

public interface ReviewDetailRepositoryCustom {
    ReviewDetail trashed(Long reviewDetailId);

    ReviewDetail restore(Long reviewDetailId);

    boolean deletePermanent(Long reviewDetailId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
