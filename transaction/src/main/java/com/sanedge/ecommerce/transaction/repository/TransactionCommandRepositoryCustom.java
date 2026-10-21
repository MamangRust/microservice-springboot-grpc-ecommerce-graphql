package com.sanedge.ecommerce.transaction.repository;

import com.sanedge.ecommerce.transaction.models.Transaction;

public interface TransactionCommandRepositoryCustom {
    Transaction trashed(Long transactionId);

    Transaction restore(Long transactionId);

    Transaction deletePermanent(Long transactionId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();

    boolean deleteByOrderPermanent(Integer orderId);
}
