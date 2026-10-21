package com.sanedge.ecommerce.gateway.graphql;

import java.util.List;

import com.sanedge.ecommerce.common.domain.responses.api.PaginationMeta;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;

/**
 * Small helpers reused by the GraphQL resolvers, mirroring the {@code nvl}
 * helpers that used to live in each REST controller.
 */
public final class Gql {

    private Gql() {
    }

    public static int i(Integer value) {
        return value == null ? 0 : value;
    }

    public static long l(Long value) {
        return value == null ? 0L : value;
    }

    public static String s(String value) {
        return value == null ? "" : value;
    }

    public static Boolean ok(String status) {
        return "success".equalsIgnoreCase(status);
    }

    public static <T> Page<T> page(List<T> data, PaginationMeta pagination) {
        return new Page<>(data, pagination);
    }
}
