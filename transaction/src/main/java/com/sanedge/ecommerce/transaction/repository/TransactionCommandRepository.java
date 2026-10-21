package com.sanedge.ecommerce.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.transaction.models.Transaction;

@Repository
public interface TransactionCommandRepository
        extends JpaRepository<Transaction, Long>, TransactionCommandRepositoryCustom {
}
