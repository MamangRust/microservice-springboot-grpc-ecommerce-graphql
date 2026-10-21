package com.sanedge.ecommerce.merchant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sanedge.ecommerce.common.chaos.ChaosManager;
import com.sanedge.ecommerce.common.outbox.KafkaOutboxPublisher;
import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;
import com.sanedge.ecommerce.merchant.models.MerchantOutbox;
import com.sanedge.ecommerce.merchant.repository.MerchantOutboxRepository;

@Component
public class MerchantOutboxPublisher extends KafkaOutboxPublisher<MerchantOutbox> {

    private final MerchantOutboxRepository repository;

    @Value("${merchant.outbox.batch-size:50}")
    private int batchSize;

    public MerchantOutboxPublisher(MerchantOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ChaosManager chaosManager) {
        super(kafkaTemplate, chaosManager);
        this.repository = repository;
    }

    @Override
    protected String moduleName() {
        return "merchant";
    }

    @Override
    protected OutboxEventRepository<MerchantOutbox> repository() {
        return repository;
    }

    @Override
    protected int batchSize() {
        return batchSize;
    }
}
