package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.category.CategoryResponse;
import com.sanedge.ecommerce.gateway.dto.responses.category.CategoryResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.category.CategoryCommandServiceGrpc;
import com.sanedge.ecommerce.pb.category.CategoryQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CategoryResolver {

    private final CategoryQueryServiceGrpc.CategoryQueryServiceBlockingStub categoryQueryStub;
    private final CategoryCommandServiceGrpc.CategoryCommandServiceBlockingStub categoryCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<CategoryResponse> categories(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = categoryQueryStub.findAll(com.sanedge.ecommerce.pb.category.FindAllCategoryRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.categories(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public CategoryResponse category(@Argument Long id) {
        var resp = categoryQueryStub.findById(com.sanedge.ecommerce.pb.category.FindByIdCategoryRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.category(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<CategoryResponseDeleteAt> activeCategories(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = categoryQueryStub.findByActive(
                com.sanedge.ecommerce.pb.category.FindAllCategoryRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.categoriesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<CategoryResponseDeleteAt> trashedCategories(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = categoryQueryStub.findByTrashed(
                com.sanedge.ecommerce.pb.category.FindAllCategoryRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.categoriesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public CategoryResponse createCategory(@Argument("input") CreateCategory input) {
        var resp = categoryCommandStub.create(com.sanedge.ecommerce.pb.category.CreateCategoryRequest.newBuilder()
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setSlugCategory(Gql.s(input.slugCategory()))
                .setImageCategory(Gql.s(input.imageCategory()))
                .build());
        return ProtoMapper.category(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public CategoryResponse updateCategory(@Argument("input") UpdateCategory input) {
        var resp = categoryCommandStub.update(com.sanedge.ecommerce.pb.category.UpdateCategoryRequest.newBuilder()
                .setCategoryId(Gql.i(input.categoryId() == null ? null : input.categoryId().intValue()))
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setSlugCategory(Gql.s(input.slugCategory()))
                .setImageCategory(Gql.s(input.imageCategory()))
                .build());
        return ProtoMapper.category(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public CategoryResponseDeleteAt trashCategory(@Argument Long id) {
        var resp = categoryCommandStub.trashedCategory(
                com.sanedge.ecommerce.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.categoryDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public CategoryResponseDeleteAt restoreCategory(@Argument Long id) {
        var resp = categoryCommandStub.restoreCategory(
                com.sanedge.ecommerce.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.categoryDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteCategoryPermanent(@Argument Long id) {
        var resp = categoryCommandStub.deleteCategoryPermanent(
                com.sanedge.ecommerce.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllCategories() {
        var resp = categoryCommandStub.restoreAllCategory(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllCategoriesPermanent() {
        var resp = categoryCommandStub.deleteAllCategoryPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateCategory(String name, String description, String slugCategory, String imageCategory) {
    }

    public record UpdateCategory(Long categoryId, String name, String description, String slugCategory,
            String imageCategory) {
    }
}
