package com.example.ordersystem.ordering.dto;

import com.example.ordersystem.ordering.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResDto {
    private Long id;
    private String memberEmail;
    private OrderStatus orderStatus;
    private List<OrderDetailDto> orderDetailDtos;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailDto {
        private Long id;
        private String productName;
        private Integer count;
    }
}
