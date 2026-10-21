package com.sanedge.ecommerce.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sanedge.ecommerce.common.chaos.ChaosManager;
import com.sanedge.ecommerce.common.outbox.KafkaOutboxPublisher;
import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;
import com.sanedge.ecommerce.order.models.OrderOutbox;
import com.sanedge.ecommerce.order.repository.OrderOutboxRepository;

@Component
public class OrderOutboxPublisher extends KafkaOutboxPublisher<OrderOutbox> {

    private final OrderOutboxRepository repository;

    @Value("${order.outbox.batch-size:50}")
    private int batchSize;

    public OrderOutboxPublisher(OrderOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ChaosManager chaosManager) {
        super(kafkaTemplate, chaosManager);
        this.repository = repository;
    }

    @Override
    protected String moduleName() {
        return "order";
    }

    @Override
    protected OutboxEventRepository<OrderOutbox> repository() {
        return repository;
    }

    @Override
    protected int batchSize() {
        return batchSize;
    }
}
