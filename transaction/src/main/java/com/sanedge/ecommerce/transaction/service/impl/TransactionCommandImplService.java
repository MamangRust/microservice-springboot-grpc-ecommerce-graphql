package com.sanedge.ecommerce.transaction.service.impl;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.ecommerce.common.adapter.merchant.MerchantPort;
import com.sanedge.ecommerce.common.adapter.model.Order;
import com.sanedge.ecommerce.common.adapter.model.OrderItem;
import com.sanedge.ecommerce.common.adapter.model.User;
import com.sanedge.ecommerce.common.adapter.order.OrderPort;
import com.sanedge.ecommerce.common.adapter.order_item.OrderItemPort;
import com.sanedge.ecommerce.common.adapter.shipping_address.ShippingAddressPort;
import com.sanedge.ecommerce.common.adapter.user.UserPort;
import com.sanedge.ecommerce.common.domain.responses.api.ApiResponse;
import com.sanedge.ecommerce.common.enums.PaymentStatus;
import com.sanedge.ecommerce.common.exception.ResourceNotFoundException;
import com.sanedge.ecommerce.common.observability.TracingMetrics;
import com.sanedge.ecommerce.common.outbox.OutboxEventFactory;
import com.sanedge.ecommerce.common.outbox.OutboxTransactions;
import com.sanedge.ecommerce.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.ecommerce.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.ecommerce.transaction.domain.responses.TransactionResponse;
import com.sanedge.ecommerce.transaction.domain.responses.TransactionResponseDeleteAt;
import com.sanedge.ecommerce.transaction.models.Transaction;
import com.sanedge.ecommerce.transaction.models.TransactionOutbox;
import com.sanedge.ecommerce.transaction.repository.TransactionCommandRepository;
import com.sanedge.ecommerce.transaction.repository.TransactionOutboxRepository;
import com.sanedge.ecommerce.transaction.repository.TransactionQueryRepository;
import com.sanedge.ecommerce.transaction.service.TransactionCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.common.Attributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionCommandImplService implements TransactionCommandService {

    private static final String TOPIC_TRANSACTION_CREATE = "email-service-topic-transaction-create";

    private final TransactionQueryRepository transactionQueryRepository;
    private final TransactionCommandRepository transactionCommandRepository;
    private final TransactionOutboxRepository transactionOutboxRepository;
    private final MerchantPort merchantPort;
    private final OrderPort orderPort;
    private final OrderItemPort orderItemPort;
    private final ShippingAddressPort shippingPort;
    private final UserPort userPort;
    private final TracingMetrics tracingMetrics;

    /**
     * Creates the payment transaction together with its outbox events.
     *
     * <p>Runs in one database transaction so the transaction row and the
     * {@code email-service-topic-transaction-create} / {@code stats.transaction.event}
     * rows are committed together.
     */
    @Override
    @Transactional
    public ApiResponse<TransactionResponse> create(CreateTransactionRequest req) {
        Attributes attrs = Attributes.builder()
                .put("order.id", req.getOrderID() != null ? req.getOrderID().toString() : "null")
                .put("merchant.id", req.getMerchantID() != null ? req.getMerchantID().toString() : "null")
                .build();

        return tracingMetrics.traceAndMeasure("createTransaction", "create_transaction", attrs, () -> {
            log.info("💳 Creating new transaction | orderId={}, merchantId={}", req.getOrderID(), req.getMerchantID());

            if (!merchantExists(req.getMerchantID())) {
                log.error("❌ Merchant not found | merchantId={}", req.getMerchantID());
                return error("Merchant not found");
            }

            if (!orderExists(req.getOrderID())) {
                log.error("❌ Order not found | orderId={}", req.getOrderID());
                return error("Order not found");
            }

            List<OrderItem> orderItems = fetchOrderItems(req.getOrderID());
            if (orderItems.isEmpty()) {
                log.error("❌ No order items found | orderId={}", req.getOrderID());
                return error("No order items found");
            }

            Integer shippingCost = fetchShippingCost(req.getOrderID());
            if (shippingCost == null) {
                log.error("❌ Shipping address not found | orderId={}", req.getOrderID());
                return error("Shipping address not found");
            }

            int totalAmount = 0;
            for (OrderItem item : orderItems) {
                if (item.quantity() <= 0) {
                    return error("Invalid order item quantity");
                }
                totalAmount += item.price() * item.quantity();
            }
            totalAmount += shippingCost;
            int ppn = totalAmount * 11 / 100;
            int totalAmountWithTax = totalAmount + ppn;

            String paymentStatus = req.getAmount() >= totalAmountWithTax ? "success" : "failed";
            if (paymentStatus.equals("failed")) {
                log.error("❌ Insufficient payment amount | amount={}, required={}", req.getAmount(),
                        totalAmountWithTax);
                return error("Insufficient payment amount");
            }

            req.setAmount(totalAmountWithTax);
            req.setPaymentStatus(paymentStatus);

            try {
                Transaction transaction = new Transaction();
                transaction.setOrderId(req.getOrderID());
                transaction.setMerchantId(req.getMerchantID());
                transaction.setPaymentMethod(req.getPaymentMethod());
                transaction.setAmount(req.getAmount());

                PaymentStatus status = PaymentStatus.fromValue(req.getPaymentStatus());

                transaction.setStatus(status);

                transaction = transactionCommandRepository.save(transaction);
                log.info("✅ Transaction created successfully | transactionId={}", transaction.getTransactionId());


                sendTransactionEmail(transaction, req.getOrderID());

                publishStatsEvent(transaction);

                return ApiResponse.<TransactionResponse>builder()
                        .status("success")
                        .message("Transaction created successfully")
                        .data(TransactionResponse.from(transaction))
                        .build();
            } catch (Exception e) {
                log.error("💥 Failed to create transaction", e);
                // The transaction and its outbox events must not be committed half-way.
                OutboxTransactions.markRollbackOnly();
                return error("Failed to create transaction");
            }
        });
    }

    @Override
    public ApiResponse<TransactionResponse> update(UpdateTransactionRequest req) {
        Attributes attrs = Attributes.builder()
                .put("transaction.id", req.getTransactionID() != null ? req.getTransactionID().toString() : "null")
                .build();

        return tracingMetrics.traceAndMeasure("updateTransaction", "update_transaction", attrs, () -> {
            log.info("✏️ Updating transaction | transactionId={}", req.getTransactionID());

            Transaction existingTx = transactionQueryRepository.findById(req.getTransactionID().longValue())
                    .orElse(null);
            if (existingTx == null) {
                log.error("❌ Transaction not found | transactionId={}", req.getTransactionID());
                return error("Transaction not found");
            }

            if (PaymentStatus.SUCCESS.equals(existingTx.getStatus()) ||
                    PaymentStatus.REFUNDED.equals(existingTx.getStatus())) {

                log.error("❌ Transaction cannot be modified | transactionId={}", req.getTransactionID());
                return error("Transaction cannot be modified");
            }

            if (!merchantExists(req.getMerchantID())) {
                log.error("❌ Merchant not found | merchantId={}", req.getMerchantID());
                return error("Merchant not found");
            }

            if (!orderExists(req.getOrderID())) {
                log.error("❌ Order not found | orderId={}", req.getOrderID());
                return error("Order not found");
            }

            List<OrderItem> orderItems = fetchOrderItems(req.getOrderID());
            if (orderItems.isEmpty()) {
                log.error("❌ No order items found | orderId={}", req.getOrderID());
                return error("No order items found");
            }

            Integer shippingCost = fetchShippingCost(req.getOrderID());
            if (shippingCost == null) {
                log.error("❌ Shipping address not found | orderId={}", req.getOrderID());
                return error("Shipping address not found");
            }

            int totalAmount = 0;
            for (OrderItem item : orderItems) {
                if (item.quantity() <= 0) {
                    return error("Invalid order item quantity");
                }
                totalAmount += item.price() * item.quantity();
            }
            totalAmount += shippingCost;
            int ppn = totalAmount * 11 / 100;
            int totalAmountWithTax = totalAmount + ppn;

            String paymentStatus = req.getAmount() >= totalAmountWithTax ? "success" : "failed";
            if (paymentStatus.equals("failed")) {
                log.error("❌ Insufficient payment amount | amount={}, required={}", req.getAmount(),
                        totalAmountWithTax);
                return error("Insufficient payment amount");
            }

            req.setAmount(totalAmountWithTax);
            req.setPaymentStatus(paymentStatus);

            try {
                existingTx.setOrderId(req.getOrderID());
                existingTx.setMerchantId(req.getMerchantID());
                existingTx.setPaymentMethod(req.getPaymentMethod());
                existingTx.setAmount(req.getAmount());

                PaymentStatus status = PaymentStatus.fromValue(req.getPaymentStatus());

                existingTx.setStatus(status);

                Transaction transaction = transactionCommandRepository.save(existingTx);
                log.info("✅ Transaction updated successfully | transactionId={}", transaction.getTransactionId());
                return ApiResponse.<TransactionResponse>builder()
                        .status("success")
                        .message("Transaction updated successfully")
                        .data(TransactionResponse.from(transaction))
                        .build();
            } catch (Exception e) {
                log.error("💥 Failed to update transaction", e);
                return error("Failed to update transaction");
            }
        });
    }

    @Override
    public ApiResponse<TransactionResponseDeleteAt> trash(Integer id) {
        Attributes attrs = Attributes.builder()
                .put("transaction.id", String.valueOf(id))
                .build();

        return tracingMetrics.traceAndMeasure("trashTransaction", "trash_transaction", attrs, () -> {
            log.info("🗑️ Trashing transaction id={}", id);
            try {
                Transaction transaction = transactionCommandRepository.trashed(id.longValue());
                return ApiResponse.<TransactionResponseDeleteAt>builder()
                        .status("success")
                        .message("🗑️ Transaction trashed successfully!")
                        .data(TransactionResponseDeleteAt.from(transaction))
                        .build();
            } catch (Exception e) {
                log.error("💥 Failed to trash transaction id={}", id, e);
                return ApiResponse.<TransactionResponseDeleteAt>builder()
                        .status("error")
                        .message("Failed to trash transaction. Please try again later.")
                        .data(null)
                        .build();
            }
        });
    }

    @Override
    public ApiResponse<TransactionResponseDeleteAt> restore(Integer id) {
        Attributes attrs = Attributes.builder()
                .put("transaction.id", String.valueOf(id))
                .build();

        return tracingMetrics.traceAndMeasure("restoreTransaction", "restore_transaction", attrs, () -> {
            log.info("♻️ Restoring transaction id={}", id);
            try {
                Transaction transaction = transactionCommandRepository.restore(id.longValue());
                return ApiResponse.<TransactionResponseDeleteAt>builder()
                        .status("success")
                        .message("♻️ Transaction restored successfully!")
                        .data(TransactionResponseDeleteAt.from(transaction))
                        .build();
            } catch (Exception e) {
                log.error("💥 Failed to restore transaction id={}", id, e);
                return ApiResponse.<TransactionResponseDeleteAt>builder()
                        .status("error")
                        .message("Failed to restore transaction. Please try again later.")
                        .data(null)
                        .build();
            }
        });
    }

    @Override
    public ApiResponse<Boolean> delete(Integer id) {
        Attributes attrs = Attributes.builder()
                .put("transaction.id", String.valueOf(id))
                .build();

        return tracingMetrics.traceAndMeasure("deleteTransactionPermanent", "delete_transaction_permanent", attrs,
                () -> {
                    log.info("🧨 Permanently deleting transaction id={}", id);
                    try {
                        transactionCommandRepository.deletePermanent(id.longValue());
                        return ApiResponse.<Boolean>builder()
                                .status("success")
                                .message("🧨 Transaction permanently deleted!")
                                .data(true)
                                .build();
                    } catch (Exception e) {
                        log.error("💥 Failed to permanently delete transaction id={}", id, e);
                        return ApiResponse.<Boolean>builder()
                                .status("error")
                                .message("Failed to permanently delete transaction. Please try again later.")
                                .data(false)
                                .build();
                    }
                });
    }

    @Override
    public ApiResponse<Boolean> restoreAll() {
        return tracingMetrics.traceAndMeasure("restoreAllTransactions", "restore_all_transactions", () -> {
            log.info("🔄 Restoring ALL trashed transactions");
            try {
                transactionCommandRepository.restoreAllDeleted();
                return ApiResponse.<Boolean>builder()
                        .status("success")
                        .message("🔄 All transactions restored successfully!")
                        .data(true)
                        .build();
            } catch (Exception e) {
                log.error("💥 Failed to restore all transactions", e);
                return ApiResponse.<Boolean>builder()
                        .status("error")
                        .message("Failed to restore all transactions. Please try again later.")
                        .data(false)
                        .build();
            }
        });
    }

    @Override
    public ApiResponse<Boolean> deleteAll() {
        return tracingMetrics.traceAndMeasure("deleteAllTransactionsPermanent", "delete_all_transactions_permanent",
                () -> {
                    log.info("💣 Permanently deleting ALL trashed transactions");
                    try {
                        transactionCommandRepository.deleteAllDeleted();
                        return ApiResponse.<Boolean>builder()
                                .status("success")
                                .message("💣 All transactions permanently deleted!")
                                .data(true)
                                .build();
                    } catch (Exception e) {
                        log.error("💥 Failed to delete all transactions", e);
                        return ApiResponse.<Boolean>builder()
                                .status("error")
                                .message("Failed to delete all transactions. Please try again later.")
                                .data(false)
                                .build();
                    }
                });
    }

    @Override
    public ApiResponse<Boolean> deleteByOrderPermanent(Integer orderId) {
        Attributes attrs = Attributes.builder()
                .put("order.id", String.valueOf(orderId))
                .build();

        return tracingMetrics.traceAndMeasure("deleteTransactionByOrderPermanent",
                "delete_transaction_by_order_permanent", attrs, () -> {
                    log.info("🧨 Permanently deleting transactions by order_id={}", orderId);
                    try {
                        transactionCommandRepository.deleteByOrderPermanent(orderId);
                        return ApiResponse.<Boolean>builder()
                                .status("success")
                                .message("🧨 Transactions by order permanently deleted!")
                                .data(true)
                                .build();
                    } catch (Exception e) {
                        log.error("💥 Failed to permanently delete transactions by order_id={}", orderId, e);
                        return ApiResponse.<Boolean>builder()
                                .status("error")
                                .message("Failed to permanently delete transactions by order. Please try again later.")
                                .data(false)
                                .build();
                    }
                });
    }

    private boolean merchantExists(Integer merchantId) {
        try {
            merchantPort.findById(merchantId);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private boolean orderExists(Integer orderId) {
        try {
            orderPort.findById(orderId);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private List<OrderItem> fetchOrderItems(Integer orderId) {
        try {
            return orderItemPort.findOrderItemByOrder(orderId);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return List.of();
            }
            throw e;
        }
    }

    private Integer fetchShippingCost(Integer orderId) {
        try {
            var shipping = shippingPort.findByOrder(orderId);
            if (shipping == null) {
                return null;
            }
            return shipping.shippingCost();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private ApiResponse<TransactionResponse> error(String message) {
        return ApiResponse.<TransactionResponse>builder()
                .status("error")
                .message(message)
                .data(null)
                .build();
    }

    /**
     * Enqueues the transaction receipt email.
     *
     * <p>The remote lookups are best effort: if the order/user service cannot be
     * reached the email is skipped. The outbox write itself is not best effort —
     * it joins the caller's transaction, so a failed write aborts it.
     */
    void sendTransactionEmail(Transaction tx, Integer orderId) {
        Order order;
        User user;
        try {
            order = orderPort.findById(orderId);
            user = userPort.findById(order.userId());
        } catch (RuntimeException e) {
            log.warn("⚠️ Skipping transaction email | transactionId={} orderId={}: {}",
                    tx.getTransactionId(), orderId, e.getMessage());
            return;
        }

        String subject = "Transaction Created Successfully";
        String body = String.format(
                "Hello %s %s,\n\nYour transaction has been created successfully!\n\nTransaction Details:\n- ID: %s\n- Amount: %s\n- Payment Method: %s\n- Status: %s\n\nThank you for shopping with us!\n\nRegards,\nSupport Team",
                user.firstname(), user.lastname(), tx.getTransactionId(), tx.getAmount(),
                tx.getPaymentMethod(), tx.getStatus());

        OutboxEventFactory.Envelope envelope = OutboxEventFactory.email(
                TOPIC_TRANSACTION_CREATE, user.email(), subject, body);

        TransactionOutbox event = new TransactionOutbox();
        event.setEventId(envelope.eventId());
        event.setTopic(TOPIC_TRANSACTION_CREATE);
        event.setEventKey(user.email());
        event.setPayload(envelope.payload());
        transactionOutboxRepository.enqueue(event);
    }

    /**
     * Enqueues {@code stats.transaction.event} for the created transaction.
     *
     * <p>The row joins the caller's transaction; a failed write aborts it, so the
     * analytics event can never be missing for a committed transaction.
     */
    void publishStatsEvent(Transaction tx) {
        YearMonth period = tx.getCreatedAt() != null
                ? YearMonth.from(tx.getCreatedAt().toLocalDateTime())
                : YearMonth.now();
        double amount = tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0;
        long merchantId = tx.getMerchantId() != null ? tx.getMerchantId() : 0L;

        OutboxEventFactory.Envelope envelope = OutboxEventFactory.stats(
                "stats.transaction.event", period.toString(), amount, merchantId, 0L);

        TransactionOutbox event = new TransactionOutbox();
        event.setEventId(envelope.eventId());
        event.setTopic("stats.transaction.event");
        event.setEventKey(String.valueOf(merchantId));
        event.setPayload(envelope.payload());
        transactionOutboxRepository.enqueue(event);
    }
}
