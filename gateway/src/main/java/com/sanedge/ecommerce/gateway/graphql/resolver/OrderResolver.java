package com.sanedge.ecommerce.gateway.graphql.resolver;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.order.OrderResponse;
import com.sanedge.ecommerce.gateway.dto.responses.order.OrderResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.order.OrderCommandServiceGrpc;
import com.sanedge.ecommerce.pb.order.OrderQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderResolver {

    private final OrderQueryServiceGrpc.OrderQueryServiceBlockingStub orderQueryStub;
    private final OrderCommandServiceGrpc.OrderCommandServiceBlockingStub orderCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<OrderResponse> orders(@Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = orderQueryStub.findAll(com.sanedge.ecommerce.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.orders(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public OrderResponse order(@Argument Long id) {
        var resp = orderQueryStub.findById(com.sanedge.ecommerce.pb.order.FindByIdOrderRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.order(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<OrderResponseDeleteAt> activeOrders(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = orderQueryStub.findByActive(com.sanedge.ecommerce.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.ordersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<OrderResponseDeleteAt> trashedOrders(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = orderQueryStub.findByTrashed(com.sanedge.ecommerce.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.ordersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public OrderResponse createOrder(@Argument("input") CreateOrder input) {
        var builder = com.sanedge.ecommerce.pb.order.CreateOrderRequest.newBuilder()
                .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                .setUserId(Gql.i(input.userId() == null ? null : input.userId().intValue()));
        if (input.items() != null) {
            input.items().forEach(item -> builder
                    .addItems(com.sanedge.ecommerce.pb.order.CreateOrderItemRequest.newBuilder()
                            .setProductId(Gql.i(item.productId() == null ? null : item.productId().intValue()))
                            .setQuantity(Gql.i(item.quantity()))
                            .setPrice(Gql.i(item.price()))
                            .build()));
        }
        if (input.shippingAddress() != null) {
            builder.setShipping(toProtoCreateShipping(input.shippingAddress()));
        }
        var resp = orderCommandStub.create(builder.build());
        return ProtoMapper.order(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public OrderResponse updateOrder(@Argument("input") UpdateOrder input) {
        var builder = com.sanedge.ecommerce.pb.order.UpdateOrderRequest.newBuilder()
                .setOrderId(Gql.i(input.orderId() == null ? null : input.orderId().intValue()))
                .setUserId(Gql.i(input.userId() == null ? null : input.userId().intValue()));
        if (input.items() != null) {
            input.items().forEach(item -> builder
                    .addItems(com.sanedge.ecommerce.pb.order.UpdateOrderItemRequest.newBuilder()
                            .setOrderItemId(Gql.i(item.orderItemId() == null ? null : item.orderItemId().intValue()))
                            .setProductId(Gql.i(item.productId() == null ? null : item.productId().intValue()))
                            .setQuantity(Gql.i(item.quantity()))
                            .setPrice(Gql.i(item.price()))
                            .build()));
        }
        if (input.shippingAddress() != null) {
            builder.setShipping(toProtoUpdateShipping(input.shippingAddress()));
        }
        var resp = orderCommandStub.update(builder.build());
        return ProtoMapper.order(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public OrderResponseDeleteAt trashOrder(@Argument Long id) {
        var resp = orderCommandStub.trashedOrder(com.sanedge.ecommerce.pb.order.FindByIdOrderRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.orderDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public OrderResponseDeleteAt restoreOrder(@Argument Long id) {
        var resp = orderCommandStub.restoreOrder(com.sanedge.ecommerce.pb.order.FindByIdOrderRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.orderDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteOrderPermanent(@Argument Long id) {
        var resp = orderCommandStub.deleteOrderPermanent(com.sanedge.ecommerce.pb.order.FindByIdOrderRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllOrders() {
        var resp = orderCommandStub.restoreAllOrder(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllOrdersPermanent() {
        var resp = orderCommandStub.deleteAllOrderPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    private static com.sanedge.ecommerce.pb.shipping_address.CreateShippingAddressRequest toProtoCreateShipping(
            CreateShippingAddress s) {
        return com.sanedge.ecommerce.pb.shipping_address.CreateShippingAddressRequest.newBuilder()
                .setOrderId(Gql.i(s.orderId() == null ? null : s.orderId().intValue()))
                .setAlamat(Gql.s(s.alamat()))
                .setProvinsi(Gql.s(s.provinsi()))
                .setKota(Gql.s(s.kota()))
                .setCourier(Gql.s(s.courier()))
                .setShippingMethod(Gql.s(s.shippingMethod()))
                .setShippingCost(Gql.i(s.shippingCost()))
                .setNegara(Gql.s(s.negara()))
                .build();
    }

    private static com.sanedge.ecommerce.pb.shipping_address.UpdateShippingAddressRequest toProtoUpdateShipping(
            UpdateShippingAddress s) {
        return com.sanedge.ecommerce.pb.shipping_address.UpdateShippingAddressRequest.newBuilder()
                .setShippingId(Gql.i(s.shippingId() == null ? null : s.shippingId().intValue()))
                .setOrderId(Gql.i(s.orderId() == null ? null : s.orderId().intValue()))
                .setAlamat(Gql.s(s.alamat()))
                .setProvinsi(Gql.s(s.provinsi()))
                .setKota(Gql.s(s.kota()))
                .setCourier(Gql.s(s.courier()))
                .setShippingMethod(Gql.s(s.shippingMethod()))
                .setShippingCost(Gql.i(s.shippingCost()))
                .setNegara(Gql.s(s.negara()))
                .build();
    }

    public record OrderItemInput(Long productId, Integer quantity, Integer price) {
    }

    public record UpdateOrderItemInput(Long orderItemId, Long productId, Integer quantity, Integer price) {
    }

    public record CreateShippingAddress(Long orderId, String alamat, String provinsi, String kota, String courier,
            String shippingMethod, Integer shippingCost, String negara) {
    }

    public record UpdateShippingAddress(Long shippingId, Long orderId, String alamat, String provinsi, String kota,
            String courier, String shippingMethod, Integer shippingCost, String negara) {
    }

    public record CreateOrder(Long merchantId, Long userId, List<OrderItemInput> items,
            CreateShippingAddress shippingAddress) {
    }

    public record UpdateOrder(Long orderId, Long userId, List<UpdateOrderItemInput> items,
            UpdateShippingAddress shippingAddress) {
    }
}
