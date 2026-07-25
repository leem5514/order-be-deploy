package com.example.ordersystem.ordering.service;

import com.example.ordersystem.common.configs.RabbitMqConfig;
import com.example.ordersystem.ordering.dto.StockDecreaseEvent;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

// redis 로 우선 처리한 재고 차감을 RabbitMQ 를 통해 비동기로 RDB 에 반영한다.
// (Redis 는 응답 속도를 위한 실시간 재고, RDB 는 최종 정합성을 위한 기준 데이터)
@Slf4j
@Component
public class StockDecreaseEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ProductRepository productRepository;

    public StockDecreaseEventHandler(RabbitTemplate rabbitTemplate, ProductRepository productRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.productRepository = productRepository;
    }

    public void publish(StockDecreaseEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.STOCK_EXCHANGE,
                RabbitMqConfig.STOCK_DECREASE_ROUTING_KEY,
                event
        );
        log.info("재고 차감 이벤트 발행 - productId: {}, quantity: {}", event.getProductId(), event.getProductCount());
    }

    // 트랜잭션 완료 이후 메시지를 수신하므로 동시성 이슈 없이 안전하게 RDB 재고를 갱신한다.
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE)
    public void listen(StockDecreaseEvent event) {
        try {
            Product product = productRepository.findById(event.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
            product.updateStockQuantity(event.getProductCount());
            log.info("재고 차감 이벤트 처리 완료 - productId: {}, quantity: {}", event.getProductId(), event.getProductCount());
        } catch (Exception e) {
            log.error("재고 차감 이벤트 처리 실패 - productId: {}, error: {}", event.getProductId(), e.getMessage());
            // 재시도해도 실패가 반복될 오류이므로 requeue 하지 않고 DLQ 로 보낸다.
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
