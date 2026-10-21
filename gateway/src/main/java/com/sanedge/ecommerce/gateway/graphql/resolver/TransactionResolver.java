package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.transaction.TransactionResponse;
import com.sanedge.ecommerce.gateway.dto.responses.transaction.TransactionResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.transaction.TransactionCommandServiceGrpc;
import com.sanedge.ecommerce.pb.transaction.TransactionQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TransactionResolver {

    private final TransactionQueryServiceGrpc.TransactionQueryServiceBlockingStub transactionQueryStub;
    private final TransactionCommandServiceGrpc.TransactionCommandServiceBlockingStub transactionCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<TransactionResponse> transactions(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = transactionQueryStub.findAllTransactions(
                com.sanedge.ecommerce.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.transactions(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<TransactionResponseDeleteAt> activeTransactions(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = transactionQueryStub.findByActive(
                com.sanedge.ecommerce.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.transactionsDeleteAtFromResponse(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<TransactionResponseDeleteAt> trashedTransactions(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = transactionQueryStub.findByTrashed(
                com.sanedge.ecommerce.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.transactionsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public TransactionResponse transaction(@Argument Long id) {
        var resp = transactionQueryStub.findById(
                com.sanedge.ecommerce.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.transaction(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<TransactionResponse> transactionsByMerchant(@Argument Long merchantId, @Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = transactionQueryStub.findByMerchant(
                com.sanedge.ecommerce.pb.transaction.FindAllTransactionByMerchantRequest.newBuilder()
                        .setMerchantId(merchantId == null ? 0 : merchantId.intValue())
                        .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.transactions(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public TransactionResponse createTransaction(@Argument("input") CreateTransaction input) {
        var resp = transactionCommandStub.create(
                com.sanedge.ecommerce.pb.transaction.CreateTransactionRequest.newBuilder()
                        .setOrderId(Gql.i(input.orderId() == null ? null : input.orderId().intValue()))
                        .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                        .setPaymentMethod(Gql.s(input.paymentMethod()))
                        .setAmount(Gql.i(input.amount()))
                        .setPaymentStatus(Gql.s(input.paymentStatus()))
                        .build());
        return ProtoMapper.transaction(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public TransactionResponse updateTransaction(@Argument("input") UpdateTransaction input) {
        var resp = transactionCommandStub.update(
                com.sanedge.ecommerce.pb.transaction.UpdateTransactionRequest.newBuilder()
                        .setTransactionId(
                                Gql.i(input.transactionId() == null ? null : input.transactionId().intValue()))
                        .setOrderId(Gql.i(input.orderId() == null ? null : input.orderId().intValue()))
                        .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                        .setPaymentMethod(Gql.s(input.paymentMethod()))
                        .setAmount(Gql.i(input.amount()))
                        .setPaymentStatus(Gql.s(input.paymentStatus()))
                        .build());
        return ProtoMapper.transaction(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public TransactionResponseDeleteAt trashTransaction(@Argument Long id) {
        var resp = transactionCommandStub.trashedTransaction(
                com.sanedge.ecommerce.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.transactionDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public TransactionResponseDeleteAt restoreTransaction(@Argument Long id) {
        var resp = transactionCommandStub.restoreTransaction(
                com.sanedge.ecommerce.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.transactionDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteTransactionPermanent(@Argument Long id) {
        var resp = transactionCommandStub.deleteTransactionPermanent(
                com.sanedge.ecommerce.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllTransactions() {
        var resp = transactionCommandStub.restoreAllTransaction(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllTransactionsPermanent() {
        var resp = transactionCommandStub.deleteAllTransactionPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateTransaction(Long orderId, Long merchantId, String paymentMethod, Integer amount,
            String paymentStatus) {
    }

    public record UpdateTransaction(Long transactionId, Long orderId, Long merchantId, String paymentMethod,
            Integer amount, String paymentStatus) {
    }
}
