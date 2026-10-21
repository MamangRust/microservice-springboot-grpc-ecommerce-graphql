package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.banner.BannerResponse;
import com.sanedge.ecommerce.gateway.dto.responses.banner.BannerResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.banner.BannerCommandServiceGrpc;
import com.sanedge.ecommerce.pb.banner.BannerQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BannerResolver {

    private final BannerQueryServiceGrpc.BannerQueryServiceBlockingStub bannerQueryStub;
    private final BannerCommandServiceGrpc.BannerCommandServiceBlockingStub bannerCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<BannerResponse> banners(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = bannerQueryStub.findAll(com.sanedge.ecommerce.pb.banner.FindAllBannerRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.banners(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public BannerResponse banner(@Argument Long id) {
        var resp = bannerQueryStub.findById(com.sanedge.ecommerce.pb.banner.FindByIdBannerRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.banner(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<BannerResponseDeleteAt> activeBanners(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = bannerQueryStub.findByActive(com.sanedge.ecommerce.pb.banner.FindAllBannerRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.bannersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<BannerResponseDeleteAt> trashedBanners(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = bannerQueryStub.findByTrashed(com.sanedge.ecommerce.pb.banner.FindAllBannerRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.bannersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public BannerResponse createBanner(@Argument("input") CreateBanner input) {
        var resp = bannerCommandStub.create(com.sanedge.ecommerce.pb.banner.CreateBannerRequest.newBuilder()
                .setName(Gql.s(input.name()))
                .setStartDate(Gql.s(input.startDate()))
                .setEndDate(Gql.s(input.endDate()))
                .setStartTime(Gql.s(input.startTime()))
                .setEndTime(Gql.s(input.endTime()))
                .setIsActive(Boolean.TRUE.equals(input.isActive()))
                .build());
        return ProtoMapper.banner(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public BannerResponse updateBanner(@Argument("input") UpdateBanner input) {
        var resp = bannerCommandStub.update(com.sanedge.ecommerce.pb.banner.UpdateBannerRequest.newBuilder()
                .setBannerId(Gql.i(input.bannerId() == null ? null : input.bannerId().intValue()))
                .setName(Gql.s(input.name()))
                .setStartDate(Gql.s(input.startDate()))
                .setEndDate(Gql.s(input.endDate()))
                .setStartTime(Gql.s(input.startTime()))
                .setEndTime(Gql.s(input.endTime()))
                .setIsActive(Boolean.TRUE.equals(input.isActive()))
                .build());
        return ProtoMapper.banner(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public BannerResponseDeleteAt trashBanner(@Argument Long id) {
        var resp = bannerCommandStub.trash(com.sanedge.ecommerce.pb.banner.FindByIdBannerRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.bannerDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public BannerResponseDeleteAt restoreBanner(@Argument Long id) {
        var resp = bannerCommandStub.restore(com.sanedge.ecommerce.pb.banner.FindByIdBannerRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.bannerDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteBannerPermanent(@Argument Long id) {
        var resp = bannerCommandStub.deletePermanent(com.sanedge.ecommerce.pb.banner.FindByIdBannerRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllBanners() {
        var resp = bannerCommandStub.restoreAll(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllBannersPermanent() {
        var resp = bannerCommandStub.deleteAll(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateBanner(String name, String startDate, String endDate, String startTime, String endTime,
            Boolean isActive) {
    }

    public record UpdateBanner(Long bannerId, String name, String startDate, String endDate, String startTime,
            String endTime, Boolean isActive) {
    }
}
