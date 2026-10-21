package com.sanedge.ecommerce.transaction.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.transaction.models.Transaction;

@Repository
public interface TransactionQueryRepository extends JpaRepository<Transaction, Long> {
  @Query("""
          SELECT t FROM Transaction t
          WHERE t.deletedAt IS NULL
            AND (
                 :keyword IS NULL
                 OR LOWER(t.paymentMethod) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(t.status) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
          ORDER BY t.createdAt DESC
      """)
  Page<Transaction> findTransactions(@Param("keyword") String keyword, Pageable pageable);

  @Query("""
          SELECT t FROM Transaction t
          WHERE t.deletedAt IS NULL
            AND (
                 :keyword IS NULL OR
                 LOWER(t.paymentMethod) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(t.status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
          ORDER BY t.createdAt DESC
      """)
  Page<Transaction> findActiveTransactions(@Param("keyword") String keyword, Pageable pageable);

  @Query("""
          SELECT t FROM Transaction t
          WHERE t.deletedAt IS NOT NULL
            AND (
                  :keyword IS NULL OR
                  LOWER(t.paymentMethod) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(t.status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
          ORDER BY t.deletedAt DESC
      """)
  Page<Transaction> findTrashedTransactions(@Param("keyword") String keyword, Pageable pageable);

  @Query("""
          SELECT t FROM Transaction t
          WHERE t.deletedAt IS NULL
            AND (
                :keyword IS NULL
                OR LOWER(t.paymentMethod) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.status) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:merchantId IS NULL OR t.merchantId = :merchantId)
          ORDER BY t.createdAt DESC
      """)
  Page<Transaction> findTransactionsByMerchant(
      @Param("keyword") String keyword,
      @Param("merchantId") Integer merchantId,
      Pageable pageable);

  @Query("""
          SELECT t FROM Transaction t
          WHERE t.transactionId = :transactionId
          AND t.deletedAt IS NULL
      """)
  Optional<Transaction> findTransactionById(@Param("transactionId") Long transactionId);

  @Query("""
          SELECT t FROM Transaction t
          WHERE t.deletedAt IS NULL
          AND t.orderId = :orderId
          ORDER BY t.createdAt DESC
      """)
  Optional<Transaction> findByOrderId(@Param("orderId") Integer orderId);
}
