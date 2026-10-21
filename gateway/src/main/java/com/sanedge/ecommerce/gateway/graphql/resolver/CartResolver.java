package com.sanedge.ecommerce.gateway.graphql.resolver;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.cart.CartResponse;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.cart.CartCommandServiceGrpc;
import com.sanedge.ecommerce.pb.cart.CartQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CartResolver {

    private final CartQueryServiceGrpc.CartQueryServiceBlockingStub cartQueryStub;
    private final CartCommandServiceGrpc.CartCommandServiceBlockingStub cartCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<CartResponse> carts(@Argument Long userId, @Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = cartQueryStub.findAll(com.sanedge.ecommerce.pb.cart.FindAllCartRequest.newBuilder()
                .setUserId(userId == null ? 0 : userId.intValue())
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.carts(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public CartResponse createCart(@Argument("input") CreateCart input) {
        var resp = cartCommandStub.create(com.sanedge.ecommerce.pb.cart.CreateCartRequest.newBuilder()
                .setQuantity(Gql.i(input.quantity()))
                .setProductId(Gql.i(input.productId() == null ? null : input.productId().intValue()))
                .setUserId(Gql.i(input.userId() == null ? null : input.userId().intValue()))
                .build());
        return ProtoMapper.cart(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Boolean deleteCart(@Argument Long id) {
        var resp = cartCommandStub.delete(com.sanedge.ecommerce.pb.cart.DeleteCartRequest.newBuilder()
                .setCartId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Boolean deleteAllCarts(@Argument("input") DeleteCarts input) {
        var builder = com.sanedge.ecommerce.pb.cart.DeleteAllCartRequest.newBuilder();
        if (input.cartIds() != null) {
            builder.addAllCartIds(input.cartIds().stream().map(Long::intValue).toList());
        }
        var resp = cartCommandStub.deleteAll(builder.build());
        return Gql.ok(resp.getStatus());
    }

    public record CreateCart(Integer quantity, Long productId, Long userId) {
    }

    public record DeleteCarts(List<Long> cartIds) {
    }
}
