package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.shipping.ShippingAddressResponse;
import com.sanedge.ecommerce.gateway.dto.responses.shipping.ShippingAddressResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.shipping_address.ShippingCommandServiceGrpc;
import com.sanedge.ecommerce.pb.shipping_address.ShippingQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ShippingResolver {

    private final ShippingQueryServiceGrpc.ShippingQueryServiceBlockingStub shippingQueryStub;
    private final ShippingCommandServiceGrpc.ShippingCommandServiceBlockingStub shippingCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ShippingAddressResponse> shippingAddresses(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = shippingQueryStub.findAll(
                com.sanedge.ecommerce.pb.shipping_address.FindAllShippingRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.shippings(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<ShippingAddressResponseDeleteAt> activeShippingAddresses(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = shippingQueryStub.findByActive(
                com.sanedge.ecommerce.pb.shipping_address.FindAllShippingRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.shippingsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<ShippingAddressResponseDeleteAt> trashedShippingAddresses(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = shippingQueryStub.findByTrashed(
                com.sanedge.ecommerce.pb.shipping_address.FindAllShippingRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.shippingsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ShippingAddressResponse shippingAddress(@Argument Long shippingId) {
        var resp = shippingQueryStub.findById(
                com.sanedge.ecommerce.pb.shipping_address.FindByIdShippingRequest.newBuilder()
                        .setId(shippingId == null ? 0 : shippingId.intValue()).build());
        return ProtoMapper.shipping(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ShippingAddressResponse shippingAddressByOrder(@Argument Long orderId) {
        var resp = shippingQueryStub.findByOrder(
                com.sanedge.ecommerce.pb.shipping_address.FindByIdShippingRequest.newBuilder()
                        .setId(orderId == null ? 0 : orderId.intValue()).build());
        return ProtoMapper.shipping(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ShippingAddressResponseDeleteAt trashShippingAddress(@Argument Long shippingId) {
        var resp = shippingCommandStub.trashedShipping(
                com.sanedge.ecommerce.pb.shipping_address.FindByIdShippingRequest.newBuilder()
                        .setId(shippingId == null ? 0 : shippingId.intValue()).build());
        return ProtoMapper.shippingDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ShippingAddressResponseDeleteAt restoreShippingAddress(@Argument Long shippingId) {
        var resp = shippingCommandStub.restoreShipping(
                com.sanedge.ecommerce.pb.shipping_address.FindByIdShippingRequest.newBuilder()
                        .setId(shippingId == null ? 0 : shippingId.intValue()).build());
        return ProtoMapper.shippingDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteShippingAddressPermanent(@Argument Long shippingId) {
        var resp = shippingCommandStub.deleteShippingPermanent(
                com.sanedge.ecommerce.pb.shipping_address.FindByIdShippingRequest.newBuilder()
                        .setId(shippingId == null ? 0 : shippingId.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllShippingAddresses() {
        var resp = shippingCommandStub.restoreAllShipping(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllShippingAddressesPermanent() {
        var resp = shippingCommandStub.deleteAllShippingPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }
}
