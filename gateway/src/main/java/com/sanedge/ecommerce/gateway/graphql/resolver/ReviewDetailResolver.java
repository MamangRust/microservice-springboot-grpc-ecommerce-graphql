package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.gateway.dto.responses.reviewdetail.ReviewDetailResponse;
import com.sanedge.ecommerce.gateway.dto.responses.reviewdetail.ReviewDetailResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.review_detail.ReviewDetailCommandServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewDetailResolver {

    private final ReviewDetailCommandServiceGrpc.ReviewDetailCommandServiceBlockingStub reviewDetailCommandStub;

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewDetailResponse createReviewDetail(@Argument("input") CreateReviewDetail input) {
        var resp = reviewDetailCommandStub.create(
                com.sanedge.ecommerce.pb.review_detail.CreateReviewDetailRequest.newBuilder()
                        .setReviewId(Gql.i(input.reviewId() == null ? null : input.reviewId().intValue()))
                        .setType(Gql.s(input.type()))
                        .setUrl(Gql.s(input.url()))
                        .setCaption(Gql.s(input.caption()))
                        .build());
        return ProtoMapper.reviewDetail(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewDetailResponse updateReviewDetail(@Argument("input") UpdateReviewDetail input) {
        var resp = reviewDetailCommandStub.update(
                com.sanedge.ecommerce.pb.review_detail.UpdateReviewDetailRequest.newBuilder()
                        .setReviewDetailId(
                                Gql.i(input.reviewDetailId() == null ? null : input.reviewDetailId().intValue()))
                        .setType(Gql.s(input.type()))
                        .setUrl(Gql.s(input.url()))
                        .setCaption(Gql.s(input.caption()))
                        .build());
        return ProtoMapper.reviewDetail(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewDetailResponseDeleteAt trashReviewDetail(@Argument Long reviewDetailId) {
        var resp = reviewDetailCommandStub.trashedReviewDetail(
                com.sanedge.ecommerce.pb.review_detail.FindByIdReviewDetailRequest.newBuilder()
                        .setId(reviewDetailId == null ? 0 : reviewDetailId.intValue()).build());
        return ProtoMapper.reviewDetailDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ReviewDetailResponseDeleteAt restoreReviewDetail(@Argument Long reviewDetailId) {
        var resp = reviewDetailCommandStub.restoreReviewDetail(
                com.sanedge.ecommerce.pb.review_detail.FindByIdReviewDetailRequest.newBuilder()
                        .setId(reviewDetailId == null ? 0 : reviewDetailId.intValue()).build());
        return ProtoMapper.reviewDetailDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteReviewDetailPermanent(@Argument Long reviewDetailId) {
        var resp = reviewDetailCommandStub.deleteReviewDetailPermanent(
                com.sanedge.ecommerce.pb.review_detail.FindByIdReviewDetailRequest.newBuilder()
                        .setId(reviewDetailId == null ? 0 : reviewDetailId.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllReviewDetails() {
        var resp = reviewDetailCommandStub.restoreAllReviewDetail(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllReviewDetailsPermanent() {
        var resp = reviewDetailCommandStub.deleteAllReviewDetailPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateReviewDetail(Long reviewId, String type, String url, String caption) {
    }

    public record UpdateReviewDetail(Long reviewDetailId, String type, String url, String caption) {
    }
}
