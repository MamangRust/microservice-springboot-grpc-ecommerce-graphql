package com.sanedge.ecommerce.banner.repository;

import com.sanedge.ecommerce.banner.models.Banner;

public interface BannerCommandRepositoryCustom {
    Banner trashed(Long bannerId);

    Banner restore(Long bannerId);

    boolean deletePermanent(Long bannerId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
