package com.example.ordersystem.common.configs;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 주문 시 재고 차감을 비동기로 RDB에 반영하기 위한 RabbitMQ 설정.
// 처리 중 예외가 나면 실패 메시지는 DLQ 로 보내 유실 없이 추적할 수 있게 한다.
@Configuration
public class RabbitMqConfig {

    public static final String STOCK_EXCHANGE = "stock.exchange";
    public static final String STOCK_DECREASE_QUEUE = "stock.decrease.queue";
    public static final String STOCK_DECREASE_ROUTING_KEY = "stock.decrease";

    public static final String STOCK_DECREASE_DLX = "stock.decrease.dlx";
    public static final String STOCK_DECREASE_DLQ = "stock.decrease.queue.dlq";
    public static final String STOCK_DECREASE_DLQ_ROUTING_KEY = "stock.decrease.dlq";

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private int port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Bean
    public DirectExchange stockExchange() {
        return new DirectExchange(STOCK_EXCHANGE);
    }

    @Bean
    public Queue stockDecreaseQueue() {
        return QueueBuilder.durable(STOCK_DECREASE_QUEUE)
                .withArgument("x-dead-letter-exchange", STOCK_DECREASE_DLX)
                .withArgument("x-dead-letter-routing-key", STOCK_DECREASE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding stockDecreaseBinding() {
        return BindingBuilder.bind(stockDecreaseQueue()).to(stockExchange()).with(STOCK_DECREASE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange stockDecreaseDlx() {
        return new DirectExchange(STOCK_DECREASE_DLX);
    }

    @Bean
    public Queue stockDecreaseDlq() {
        return QueueBuilder.durable(STOCK_DECREASE_DLQ).build();
    }

    @Bean
    public Binding stockDecreaseDlqBinding() {
        return BindingBuilder.bind(stockDecreaseDlq()).to(stockDecreaseDlx()).with(STOCK_DECREASE_DLQ_ROUTING_KEY);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
