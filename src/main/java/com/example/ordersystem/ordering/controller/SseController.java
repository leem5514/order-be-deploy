package com.example.ordersystem.ordering.controller;


import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.dto.SseNotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
//import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/* 이중화 문제를 해결 하기 위해서 */
//loadBalancing 을 통해서 a,b 가 각 server1 server2  으로 이동시 같은 서버로 들어가지 않는 문제 발생

@RestController
public class SseController implements MessageListener {
    //SseEmitter 은 연결된 사용자 정보를 의미
    // ConcurrentHashMap은 Thread-SAFE 한 MAP(동시성 이슈 발생X)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final RedisMessageListenerContainer redisMessageListenerContainer;
//    private void subscribeChannel(String email) {
//        MessageListenerAdapter listenerAdapter = createList
//    }
    private Set<String> subscribeList = ConcurrentHashMap.newKeySet();

    @Qualifier("4")
    private final RedisTemplate<String,Object> sseRedisTemplate;

    public SseController(@Qualifier("4") RedisTemplate<String, Object> sseRedisTemplate, RedisMessageListenerContainer redisMessageListenerContainer) {
        this.sseRedisTemplate = sseRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }


    // email에 해당하는 메세지를 listen 하는 listener을 추가한 것
    public void subscribeChannel(String email){
        // 이미 구독한 email 일 경우에 더 이상 구독하지 않도록 처리.
        if(!subscribeList.contains(email)) {
            MessageListenerAdapter listenerAdapter = createListenerAdapter(this);
            redisMessageListenerContainer.addMessageListener(listenerAdapter, new PatternTopic(email));
            subscribeList.add(email);
        }
    }
    private MessageListenerAdapter createListenerAdapter(SseController sseController){
        return new MessageListenerAdapter(sseController, "onMessage");
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(14400*60*1000L); // 30분 정도 emitter 유효시간 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        emitters.put(email, emitter);
        emitter.onCompletion(()->emitters.remove(email));
        emitter.onTimeout(()->emitters.remove(email));
        try{
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        }catch(IOException e) {
            e.printStackTrace();
        }
        // subscribe 하자마자 listen 실시(redis 을 대상으로도)
        subscribeChannel(email);
        return emitter;

    }

    // 실 사용자에게 메세지를 전송. eventType 으로 "ordered"(신규주문, 관리자용) / "order-cancelled"(주문취소, 구매자용) 구분.
    // Redis 왕복이 주문 생성/취소 응답 속도에 영향 안 주도록 비동기로 뗀다 — 반드시 이미 완성된 DTO(더 이상
    // 지연로딩 프록시를 안 건드리는 순수 값 객체)만 넘겨받아야 한다. 엔티티 접근은 호출부에서 트랜잭션 안에 끝내야 함.
    @Async("notificationExecutor")
    public void publishMessage(OrderListResDto dto, String email, String eventType) {
        SseNotificationDto notification = SseNotificationDto.builder()
                .eventType(eventType)
                .order(dto)
                .build();
        sseRedisTemplate.convertAndSend(email, notification);
    }

    /* if 레디스에서 메세지를 확인 하고 싶으면 */
    // cli 명령어 중 -> subscribe 명령어 : 나 또한 리스너 중 1명이 되도록 설정
    // ex) subscribe admin@test.com
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Message 내용 parsing
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SseNotificationDto notification = objectMapper.readValue(message.getBody(), SseNotificationDto.class);
            String email = new String(pattern, StandardCharsets.UTF_8);
            SseEmitter emitter = emitters.get(email);
            if(emitter != null){
                emitter.send(SseEmitter.event().name(notification.getEventType()).data(notification.getOrder()));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
