package com.sanedge.ecommerce.transaction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sanedge.ecommerce.common.chaos.ChaosManager;
import com.sanedge.ecommerce.common.outbox.KafkaOutboxPublisher;
import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;
import com.sanedge.ecommerce.transaction.models.TransactionOutbox;
import com.sanedge.ecommerce.transaction.repository.TransactionOutboxRepository;

@Component
public class TransactionOutboxPublisher extends KafkaOutboxPublisher<TransactionOutbox> {

    private final TransactionOutboxRepository repository;

    @Value("${transaction.outbox.batch-size:50}")
    private int batchSize;

    public TransactionOutboxPublisher(TransactionOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ChaosManager chaosManager) {
        super(kafkaTemplate, chaosManager);
        this.repository = repository;
    }

    @Override
    protected String moduleName() {
        return "transaction";
    }

    @Override
    protected OutboxEventRepository<TransactionOutbox> repository() {
        return repository;
    }

    @Override
    protected int batchSize() {
        return batchSize;
    }
}
