package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.product.ProductResponse;
import com.sanedge.ecommerce.gateway.dto.responses.product.ProductResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.product.ProductCommandServiceGrpc;
import com.sanedge.ecommerce.pb.product.ProductQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductResolver {

    private final ProductQueryServiceGrpc.ProductQueryServiceBlockingStub productQueryStub;
    private final ProductCommandServiceGrpc.ProductCommandServiceBlockingStub productCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ProductResponse> products(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = productQueryStub.findAll(com.sanedge.ecommerce.pb.product.FindAllProductRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.products(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ProductResponse product(@Argument Long id) {
        var resp = productQueryStub.findById(com.sanedge.ecommerce.pb.product.FindByIdProductRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.product(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ProductResponse> productsByMerchant(@Argument Long merchantId, @Argument Long categoryId,
            @Argument Integer minPrice, @Argument Integer maxPrice, @Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = productQueryStub.findByMerchant(
                com.sanedge.ecommerce.pb.product.FindAllProductMerchantRequest.newBuilder()
                        .setMerchantId(merchantId == null ? 0 : merchantId.intValue())
                        .setCategoryId(categoryId == null ? 0 : categoryId.intValue())
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search))
                        .setMinPrice(Gql.i(minPrice)).setMaxPrice(Gql.i(maxPrice))
                        .build());
        return Page.of(ProtoMapper.products(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ProductResponse> productsByCategory(@Argument String categoryName, @Argument Integer minPrice,
            @Argument Integer maxPrice, @Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = productQueryStub.findByCategory(
                com.sanedge.ecommerce.pb.product.FindAllProductCategoryRequest.newBuilder()
                        .setCategoryName(Gql.s(categoryName))
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search))
                        .setMinPrice(Gql.i(minPrice)).setMaxPrice(Gql.i(maxPrice))
                        .build());
        return Page.of(ProtoMapper.products(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<ProductResponseDeleteAt> activeProducts(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = productQueryStub.findByActive(com.sanedge.ecommerce.pb.product.FindAllProductRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.productsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<ProductResponseDeleteAt> trashedProducts(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = productQueryStub.findByTrashed(com.sanedge.ecommerce.pb.product.FindAllProductRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.productsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProductResponse createProduct(@Argument("input") CreateProduct input) {
        var resp = productCommandStub.create(com.sanedge.ecommerce.pb.product.CreateProductRequest.newBuilder()
                .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                .setCategoryId(Gql.i(input.categoryId() == null ? null : input.categoryId().intValue()))
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setPrice(Gql.i(input.price()))
                .setCountInStock(Gql.i(input.countInStock()))
                .setBrand(Gql.s(input.brand()))
                .setWeight(Gql.i(input.weight()))
                .setRating(Gql.i(input.rating()))
                .setSlugProduct(Gql.s(input.slugProduct()))
                .setImageProduct(Gql.s(input.imageProduct()))
                .build());
        return ProtoMapper.product(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProductResponse updateProduct(@Argument("input") UpdateProduct input) {
        var resp = productCommandStub.update(com.sanedge.ecommerce.pb.product.UpdateProductRequest.newBuilder()
                .setProductId(Gql.i(input.productId() == null ? null : input.productId().intValue()))
                .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                .setCategoryId(Gql.i(input.categoryId() == null ? null : input.categoryId().intValue()))
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setPrice(Gql.i(input.price()))
                .setCountInStock(Gql.i(input.countInStock()))
                .setBrand(Gql.s(input.brand()))
                .setWeight(Gql.i(input.weight()))
                .setRating(Gql.i(input.rating()))
                .setSlugProduct(Gql.s(input.slugProduct()))
                .setImageProduct(Gql.s(input.imageProduct()))
                .build());
        return ProtoMapper.product(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProductResponseDeleteAt trashProduct(@Argument Long id) {
        var resp = productCommandStub.trashedProduct(
                com.sanedge.ecommerce.pb.product.FindByIdProductRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.productDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProductResponseDeleteAt restoreProduct(@Argument Long id) {
        var resp = productCommandStub.restoreProduct(
                com.sanedge.ecommerce.pb.product.FindByIdProductRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.productDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteProductPermanent(@Argument Long id) {
        var resp = productCommandStub.deleteProductPermanent(
                com.sanedge.ecommerce.pb.product.FindByIdProductRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllProducts() {
        var resp = productCommandStub.restoreAllProduct(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllProductsPermanent() {
        var resp = productCommandStub.deleteAllProductPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateProduct(Long merchantId, Long categoryId, String name, String description, Integer price,
            Integer countInStock, String brand, Integer weight, Integer rating, String slugProduct,
            String imageProduct) {
    }

    public record UpdateProduct(Long productId, Long merchantId, Long categoryId, String name, String description,
            Integer price, Integer countInStock, String brand, Integer weight, Integer rating, String slugProduct,
            String imageProduct) {
    }
}
