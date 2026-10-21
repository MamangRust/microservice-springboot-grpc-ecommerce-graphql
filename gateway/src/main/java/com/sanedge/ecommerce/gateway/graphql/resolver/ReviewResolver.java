package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.reviews.ReviewRelationsDetailResponse;
import com.sanedge.ecommerce.gateway.dto.responses.reviews.ReviewResponse;
import com.sanedge.ecommerce.gateway.dto.responses.reviews.ReviewResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.review.ReviewCommandServiceGrpc;
import com.sanedge.ecommerce.pb.review.ReviewQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewResolver {

    private final ReviewQueryServiceGrpc.ReviewQueryServiceBlockingStub reviewQueryStub;
    private final ReviewCommandServiceGrpc.ReviewCommandServiceBlockingStub reviewCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ReviewResponse> reviews(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = reviewQueryStub.findAll(com.sanedge.ecommerce.pb.review.FindAllReviewRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.reviews(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<ReviewResponseDeleteAt> activeReviews(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = reviewQueryStub.findByActive(com.sanedge.ecommerce.pb.review.FindAllReviewRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.reviewsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<ReviewResponseDeleteAt> trashedReviews(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = reviewQueryStub.findByTrashed(com.sanedge.ecommerce.pb.review.FindAllReviewRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.reviewsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ReviewRelationsDetailResponse> reviewsByProduct(@Argument Long productId, @Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = reviewQueryStub.findByProduct(
                com.sanedge.ecommerce.pb.review.FindAllReviewProductRequest.newBuilder()
                        .setProductId(productId == null ? 0 : productId.intValue())
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.reviewRelationsDetails(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<ReviewRelationsDetailResponse> reviewsByMerchant(@Argument Long merchantId, @Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = reviewQueryStub.findByMerchant(
                com.sanedge.ecommerce.pb.review.FindAllReviewMerchantRequest.newBuilder()
                        .setMerchantId(merchantId == null ? 0 : merchantId.intValue())
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.reviewRelationsDetails(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ReviewResponse review(@Argument Long reviewId) {
        var resp = reviewQueryStub.findById(com.sanedge.ecommerce.pb.review.FindByIdReviewRequest.newBuilder()
                .setId(reviewId == null ? 0 : reviewId.intValue()).build());
        return ProtoMapper.review(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewResponse createReview(@Argument("input") CreateReview input) {
        var resp = reviewCommandStub.create(com.sanedge.ecommerce.pb.review.CreateReviewRequest.newBuilder()
                .setUserId(input.userId() == null ? 0 : input.userId().intValue())
                .setProductId(input.productId() == null ? 0 : input.productId().intValue())
                .setName("")
                .setComment(Gql.s(input.comment()))
                .setRating(input.rating() == null ? 0 : input.rating())
                .build());
        return ProtoMapper.review(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewResponse updateReview(@Argument("input") UpdateReview input) {
        var resp = reviewCommandStub.update(com.sanedge.ecommerce.pb.review.UpdateReviewRequest.newBuilder()
                .setReviewId(Gql.i(input.reviewId() == null ? null : input.reviewId().intValue()))
                .setName(Gql.s(input.name()))
                .setComment(Gql.s(input.comment()))
                .setRating(input.rating() == null ? 0 : input.rating())
                .build());
        return ProtoMapper.review(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewResponseDeleteAt trashReview(@Argument Long reviewId) {
        var resp = reviewCommandStub.trashedReview(com.sanedge.ecommerce.pb.review.FindByIdReviewRequest.newBuilder()
                .setId(reviewId == null ? 0 : reviewId.intValue()).build());
        return ProtoMapper.reviewDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewResponseDeleteAt restoreReview(@Argument Long reviewId) {
        var resp = reviewCommandStub.restoreReview(com.sanedge.ecommerce.pb.review.FindByIdReviewRequest.newBuilder()
                .setId(reviewId == null ? 0 : reviewId.intValue()).build());
        return ProtoMapper.reviewDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteReviewPermanent(@Argument Long reviewId) {
        var resp = reviewCommandStub.deleteReviewPermanent(
                com.sanedge.ecommerce.pb.review.FindByIdReviewRequest.newBuilder()
                        .setId(reviewId == null ? 0 : reviewId.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllReviews() {
        var resp = reviewCommandStub.restoreAllReview(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllReviewsPermanent() {
        var resp = reviewCommandStub.deleteAllReviewPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateReview(Long userId, Long productId, Integer rating, String comment) {
    }

    public record UpdateReview(Long reviewId, String name, Integer rating, String comment) {
    }
}
