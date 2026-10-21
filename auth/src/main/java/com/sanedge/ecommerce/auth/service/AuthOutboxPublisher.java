package com.sanedge.ecommerce.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sanedge.ecommerce.auth.models.AuthOutbox;
import com.sanedge.ecommerce.auth.repository.AuthOutboxRepository;
import com.sanedge.ecommerce.common.chaos.ChaosManager;
import com.sanedge.ecommerce.common.outbox.KafkaOutboxPublisher;
import com.sanedge.ecommerce.common.outbox.OutboxEventRepository;

/**
 * Drains {@code auth_outbox} into Kafka.
 *
 * <p>Spring port of the blueprint's {@code AuthOutboxPublisher}. The polling,
 * claiming and backoff logic lives in {@link KafkaOutboxPublisher}; this class
 * only binds it to the auth table and module label.
 */
@Component
public class AuthOutboxPublisher extends KafkaOutboxPublisher<AuthOutbox> {

    private final AuthOutboxRepository repository;

    @Value("${auth.outbox.batch-size:50}")
    private int batchSize;

    public AuthOutboxPublisher(AuthOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ChaosManager chaosManager) {
        super(kafkaTemplate, chaosManager);
        this.repository = repository;
    }

    @Override
    protected String moduleName() {
        return "auth";
    }

    @Override
    protected OutboxEventRepository<AuthOutbox> repository() {
        return repository;
    }

    @Override
    protected int batchSize() {
        return batchSize;
    }
}
