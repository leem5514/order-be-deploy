package com.example.ordersystem.ordering.dto;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.ordering.domain.OrderStatus;
import com.example.ordersystem.ordering.domain.Ordering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSaveReqDto {
//    private Long memberId;
//    private List<OrderDetailDto> orderDetailDtoList;
//    private OrderStatus orderStatus;
    private Long productId;
    private int productCount;

//
//    @Builder
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class OrderDetailDto {
//        private Long productId;
//        private int productCount;
//    }
    public Ordering toEntity(Member member){
        return Ordering.builder()
                .member(member)
//            .orderstatus(OrderStatus.ORDERED)
                .build();
    }
}
