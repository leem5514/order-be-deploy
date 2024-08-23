package com.example.ordersystem.ordering.domain;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ordering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderstatus = OrderStatus.ORDERED;

    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    // 빌더패턴에서도 ArrayList 으로 초기화 되도록 하는 설정
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public void updateStock (int quantity) {

    }

    public OrderListResDto fromEntityList() {
        List<OrderDetail> orderDetailList = this.getOrderDetails();
        List<OrderListResDto.OrderDetailDto> orderDetailDtos = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetailDtos.add(orderDetail.fromEntity());
        }

        OrderListResDto orderListResDto = OrderListResDto.builder()
                .id(this.id)
                .memberEmail(this.member.getEmail())
                .orderStatus(this.orderstatus)
                .orderDetailDtos(orderDetailDtos)
                .build();
        return orderListResDto;
    }
    public void updateStatus(OrderStatus orderStatus) {
        this.orderstatus = orderStatus;
    }

}
