package com.sanedge.ecommerce.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sanedge.ecommerce.common.adapter.model.OrderItem;
import com.sanedge.ecommerce.order.models.Order;
import com.sanedge.ecommerce.order.models.OrderOutbox;
import com.sanedge.ecommerce.order.repository.OrderOutboxRepository;

/**
 * The order events are written inside the transaction that creates the order, so
 * a failing outbox write has to abort that transaction instead of dropping the
 * event silently.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCommandImplServiceOutboxTest {

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    private OrderCommandImplService service;

    @BeforeEach
    void setUp() {
        // only the outbox repository is exercised by these tests
        service = new OrderCommandImplService(null, null, orderOutboxRepository, null,
                null, null, null, null, null, null);
    }

    private Order order() {
        Order order = new Order();
        order.setOrderId(7L);
        order.setMerchantId(3);
        order.setTotalPrice(25000);
        return order;
    }

    @Test
    void orderStatsEventIsWrittenToTheOutbox() {
        service.publishOrderStatsEvent(order());

        ArgumentCaptor<OrderOutbox> captor = ArgumentCaptor.forClass(OrderOutbox.class);
        verify(orderOutboxRepository).enqueue(captor.capture());
        OrderOutbox event = captor.getValue();
        assertThat(event.getTopic()).isEqualTo("stats.ecommerce.order.event");
        assertThat(event.getEventKey()).isEqualTo("3");
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getPayload()).contains("\"order_id\":7");
    }

    @Test
    void orderItemStatsEventIsWrittenToTheOutbox() {
        OrderItem item = new OrderItem(11, 7, 5, 2, 1000, Instant.now(), Instant.now());

        service.publishOrderItemStatsEvent(3, item, 4);

        ArgumentCaptor<OrderOutbox> captor = ArgumentCaptor.forClass(OrderOutbox.class);
        verify(orderOutboxRepository).enqueue(captor.capture());
        OrderOutbox event = captor.getValue();
        assertThat(event.getTopic()).isEqualTo("stats.ecommerce.order_item.event");
        assertThat(event.getEventKey()).isEqualTo("7");
        assertThat(event.getPayload()).contains("\"order_item_id\":11");
    }

    @Test
    void aFailingOutboxWriteAbortsTheTransaction() {
        doThrow(new IllegalStateException("db down")).when(orderOutboxRepository).enqueue(any());

        assertThatThrownBy(() -> service.publishOrderStatsEvent(order()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }
}
