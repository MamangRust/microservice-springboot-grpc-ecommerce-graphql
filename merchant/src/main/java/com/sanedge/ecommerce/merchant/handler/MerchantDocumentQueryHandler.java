package com.sanedge.ecommerce.merchant.handler;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.protobuf.StringValue;
import com.sanedge.ecommerce.common.domain.responses.api.ApiResponse;
import com.sanedge.ecommerce.common.domain.responses.api.ApiResponsePagination;
import com.sanedge.ecommerce.common.grpc.GrpcErrorMapper;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.merchant.domain.responses.MerchantDocumentResponse;
import com.sanedge.ecommerce.merchant.domain.responses.MerchantDocumentResponseDeleteAt;
import com.sanedge.ecommerce.merchant.service.MerchantDocumentQueryService;
import com.sanedge.ecommerce.pb.merchant_document.ApiResponseMerchantDocument;
import com.sanedge.ecommerce.pb.merchant_document.ApiResponsePaginationMerchantDocument;
import com.sanedge.ecommerce.pb.merchant_document.ApiResponsePaginationMerchantDocumentAt;
import com.sanedge.ecommerce.pb.merchant_document.FindAllMerchantDocumentsRequest;
import com.sanedge.ecommerce.pb.merchant_document.FindMerchantDocumentByIdRequest;
import com.sanedge.ecommerce.pb.merchant_document.MerchantDocument;
import com.sanedge.ecommerce.pb.merchant_document.MerchantDocumentDeleteAt;
import com.sanedge.ecommerce.pb.merchant_document.MerchantDocumentQueryServiceGrpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

/** gRPC query surface for merchant documents (blocking style). */
@Service
@RequiredArgsConstructor
public class MerchantDocumentQueryHandler extends MerchantDocumentQueryServiceGrpc.MerchantDocumentQueryServiceImplBase {

    private final MerchantDocumentQueryService merchantDocumentQueryService;

    @Override
    public void findAll(FindAllMerchantDocumentsRequest request,
            StreamObserver<ApiResponsePaginationMerchantDocument> observer) {
        try {
            ApiResponsePagination<List<MerchantDocumentResponse>> res = merchantDocumentQueryService
                    .findAll(toDomainRequest(request));

            ApiResponsePaginationMerchantDocument.Builder builder = ApiResponsePaginationMerchantDocument.newBuilder()
                    .setStatus(ProtoMappers.nvl(res.getStatus()))
                    .setMessage(ProtoMappers.nvl(res.getMessage()))
                    .setPagination(ProtoMappers.toProtoPagination(res.getPagination()));
            res.getData().forEach(d -> builder.addData(toProto(d)));

            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatusRuntimeException(e));
        }
    }

    @Override
    public void findAllActive(FindAllMerchantDocumentsRequest request,
            StreamObserver<ApiResponsePaginationMerchantDocument> observer) {
        try {
            ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> res = merchantDocumentQueryService
                    .findAllActive(toDomainRequest(request));

            ApiResponsePaginationMerchantDocument.Builder builder = ApiResponsePaginationMerchantDocument.newBuilder()
                    .setStatus(ProtoMappers.nvl(res.getStatus()))
                    .setMessage(ProtoMappers.nvl(res.getMessage()))
                    .setPagination(ProtoMappers.toProtoPagination(res.getPagination()));
            res.getData().forEach(d -> builder.addData(toProto(d)));

            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatusRuntimeException(e));
        }
    }

    @Override
    public void findAllTrashed(FindAllMerchantDocumentsRequest request,
            StreamObserver<ApiResponsePaginationMerchantDocumentAt> observer) {
        try {
            ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> res = merchantDocumentQueryService
                    .findAllTrashed(toDomainRequest(request));

            ApiResponsePaginationMerchantDocumentAt.Builder builder = ApiResponsePaginationMerchantDocumentAt
                    .newBuilder()
                    .setStatus(ProtoMappers.nvl(res.getStatus()))
                    .setMessage(ProtoMappers.nvl(res.getMessage()))
                    .setPagination(ProtoMappers.toProtoPagination(res.getPagination()));
            res.getData().forEach(d -> builder.addData(toProtoDeleteAt(d)));

            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatusRuntimeException(e));
        }
    }

    @Override
    public void findById(FindMerchantDocumentByIdRequest request, StreamObserver<ApiResponseMerchantDocument> observer) {
        try {
            ApiResponse<MerchantDocumentResponse> res = merchantDocumentQueryService
                    .findById((long) request.getDocumentId());

            observer.onNext(ApiResponseMerchantDocument.newBuilder()
                    .setStatus(ProtoMappers.nvl(res.getStatus()))
                    .setMessage(ProtoMappers.nvl(res.getMessage()))
                    .setData(toProto(res.getData()))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatusRuntimeException(e));
        }
    }

    private static com.sanedge.ecommerce.merchant.domain.requests.FindAllMerchantDocuments toDomainRequest(
            FindAllMerchantDocumentsRequest request) {
        com.sanedge.ecommerce.merchant.domain.requests.FindAllMerchantDocuments req = new com.sanedge.ecommerce.merchant.domain.requests.FindAllMerchantDocuments();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());
        return req;
    }

    private MerchantDocument toProto(
            MerchantDocumentResponse r) {
        return MerchantDocumentCommandHandler.toProto(r);
    }

    private MerchantDocument toProto(
            MerchantDocumentResponseDeleteAt r) {
        return MerchantDocumentCommandHandler.toProto(r);
    }

    private MerchantDocumentDeleteAt toProtoDeleteAt(
            MerchantDocumentResponseDeleteAt r) {
        if (r == null) {
            return MerchantDocumentDeleteAt.getDefaultInstance();
        }
        MerchantDocumentDeleteAt.Builder builder = MerchantDocumentDeleteAt
                .newBuilder();
        if (r.getDocumentId() != null) {
            builder.setDocumentId(r.getDocumentId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        builder.setDocumentType(ProtoMappers.nvl(r.getDocumentType()));
        builder.setDocumentUrl(ProtoMappers.nvl(r.getDocumentUrl()));
        builder.setStatus(ProtoMappers.nvl(r.getStatus()));
        builder.setNote(ProtoMappers.nvl(r.getNote()));
        builder.setUploadedAt(ProtoMappers.nvl(r.getCreatedAt()));
        builder.setUpdatedAt(ProtoMappers.nvl(r.getUpdatedAt()));
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
