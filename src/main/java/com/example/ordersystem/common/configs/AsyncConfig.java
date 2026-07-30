package com.example.ordersystem.common.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

// SSE 알림 발송처럼 "실패해도 주문 자체엔 지장 없는" 부가 작업을 요청 스레드에서 떼어내기 위한 전용 스레드풀.
// 주문 생성/취소 응답 속도가 Redis pub/sub 왕복이나 지연 로딩 쿼리에 발목 잡히지 않게 한다.
// proxyTargetClass=true : SseController처럼 인터페이스(MessageListener)를 구현한 빈은 기본(JDK 동적 프록시)
// 설정으로는 구체 클래스 타입으로 주입이 안 되어 기동 자체가 실패한다. CGLIB 프록시를 강제해서 우회.
@Slf4j
@EnableAsync(proxyTargetClass = true)
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notify-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return notificationExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("비동기 처리 실패 - method: {}, error: {}", method.getName(), ex.getMessage(), ex);
    }
}
