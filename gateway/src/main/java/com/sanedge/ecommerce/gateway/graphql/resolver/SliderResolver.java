package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.slider.SliderResponse;
import com.sanedge.ecommerce.gateway.dto.responses.slider.SliderResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.slider.SliderCommandServiceGrpc;
import com.sanedge.ecommerce.pb.slider.SliderQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SliderResolver {

    private final SliderQueryServiceGrpc.SliderQueryServiceBlockingStub sliderQueryStub;
    private final SliderCommandServiceGrpc.SliderCommandServiceBlockingStub sliderCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<SliderResponse> sliders(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = sliderQueryStub.findAll(com.sanedge.ecommerce.pb.slider.FindAllSliderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.sliders(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<SliderResponseDeleteAt> activeSliders(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = sliderQueryStub.findByActive(com.sanedge.ecommerce.pb.slider.FindAllSliderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.slidersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<SliderResponseDeleteAt> trashedSliders(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = sliderQueryStub.findByTrashed(com.sanedge.ecommerce.pb.slider.FindAllSliderRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.slidersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public SliderResponse createSlider(@Argument("input") CreateSlider input) {
        var resp = sliderCommandStub.create(com.sanedge.ecommerce.pb.slider.CreateSliderRequest.newBuilder()
                .setName(Gql.s(input.name()))
                .setImage(Gql.s(input.image()))
                .build());
        return ProtoMapper.slider(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public SliderResponse updateSlider(@Argument("input") UpdateSlider input) {
        var resp = sliderCommandStub.update(com.sanedge.ecommerce.pb.slider.UpdateSliderRequest.newBuilder()
                .setId(Gql.i(input.sliderId() == null ? null : input.sliderId().intValue()))
                .setName(Gql.s(input.name()))
                .setImage(Gql.s(input.image()))
                .build());
        return ProtoMapper.slider(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public SliderResponseDeleteAt trashSlider(@Argument Long id) {
        var resp = sliderCommandStub.trashedSlider(com.sanedge.ecommerce.pb.slider.FindByIdSliderRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.sliderDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public SliderResponseDeleteAt restoreSlider(@Argument Long id) {
        var resp = sliderCommandStub.restoreSlider(com.sanedge.ecommerce.pb.slider.FindByIdSliderRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.sliderDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteSliderPermanent(@Argument Long id) {
        var resp = sliderCommandStub.deleteSliderPermanent(
                com.sanedge.ecommerce.pb.slider.FindByIdSliderRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllSliders() {
        var resp = sliderCommandStub.restoreAllSlider(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllSlidersPermanent() {
        var resp = sliderCommandStub.deleteAllSliderPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateSlider(String name, String image) {
    }

    public record UpdateSlider(Long sliderId, String name, String image) {
    }
}
