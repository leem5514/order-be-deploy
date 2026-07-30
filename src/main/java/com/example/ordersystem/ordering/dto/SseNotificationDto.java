package com.example.ordersystem.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Redis pub/sub으로 실어나르는 SSE 알림 봉투. 이벤트 종류(신규주문/취소 등)를 같이 실어서
// 구독자 쪽에서 어떤 이름의 SSE 이벤트로 내보낼지 구분할 수 있게 한다.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseNotificationDto {
    private String eventType; // "ordered" | "order-cancelled"
    private OrderListResDto order;
}
