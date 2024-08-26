//package com.example.ordersystem.ordering.service;
//
//import com.example.ordersystem.common.configs.RabbitMqConfig;
//import com.example.ordersystem.ordering.dto.StockDecreaseEvent;
//import com.example.ordersystem.product.domain.Product;
//import com.example.ordersystem.product.repository.ProductRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.persistence.EntityNotFoundException;
//
//@Component
//public class StockDecreaseEventHandler {
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    public void publish(StockDecreaseEvent event){
//        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_DECREASE_QUEUE, event);
//    }
//
//    // Transaction 완료 이후 그 다음에 메시지 수신 -> 동시성 이슈 발생 x
//    @Transactional
//    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE) // 선언된 큐만 바라보고 있다가 메세지를 받아서 redis 처리
//    public void listen(Message message){
//        String messageBody = new String(message.getBody());
//        System.out.println(messageBody);
//        // json 메세지를 ObjectMapper 으로 직접 parsing
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            StockDecreaseEvent stockDecreaseEvent = objectMapper.readValue(messageBody, StockDecreaseEvent.class);
//            // 재고 업데이트
//            Product product = productRepository.findById(stockDecreaseEvent.getProductId()).orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다.") );
//            product.updateStockQuantity(stockDecreaseEvent.getProductCount());
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//
//    }
//}