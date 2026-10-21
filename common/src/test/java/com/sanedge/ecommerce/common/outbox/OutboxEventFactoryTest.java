package com.sanedge.ecommerce.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Guards the contract with the email-service consumer: every field its
 * hasValidEnvelope() requires, plus the exact schema version it expects.
 */
class OutboxEventFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void emailEnvelopeCarriesEveryFieldTheConsumerRequires() throws Exception {
        var env = OutboxEventFactory.email(
                "email-service-topic-auth-register", "a@b.com", "Welcome", "Hello there");

        Map<String, Object> p = MAPPER.readValue(env.payload(), new TypeReference<>() {
        });

        // the six fields EmailService.hasValidEnvelope() checks
        assertThat(p.get("event_id")).isEqualTo(env.eventId());
        assertThat(p.get("schema_version")).isEqualTo(1); // must equal EVENT_SCHEMA_VERSION
        assertThat(p.get("event_type")).isEqualTo("email-service-topic-auth-register");
        assertThat(p.get("email")).isEqualTo("a@b.com");
        assertThat(p.get("subject")).isEqualTo("Welcome");
        assertThat(p.get("body")).isEqualTo("Hello there");

        // observability extra, not validated by the consumer
        assertThat(p.get("occurred_at")).isNotNull();
    }

    @Test
    void eventTypeMatchesTopicSoRoutingStaysConsistent() throws Exception {
        String topic = "email-service-topic-transaction-create";
        var env = OutboxEventFactory.email(topic, "x@y.com", "S", "B");
        Map<String, Object> p = MAPPER.readValue(env.payload(), new TypeReference<>() {
        });
        assertThat(p.get("event_type")).isEqualTo(topic);
    }

    @Test
    void eachCallGeneratesADistinctEventId() {
        var a = OutboxEventFactory.email("t", "e@e.com", "s", "b");
        var b = OutboxEventFactory.email("t", "e@e.com", "s", "b");
        assertThat(a.eventId()).isNotEqualTo(b.eventId());
    }
}
