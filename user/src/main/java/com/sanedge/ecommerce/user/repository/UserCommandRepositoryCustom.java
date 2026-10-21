package com.sanedge.ecommerce.user.repository;

import com.sanedge.ecommerce.user.models.User;

public interface UserCommandRepositoryCustom {
    User trashed(Long userId);

    User restore(Long userId);
}
