package com.example.ordersystem.ordering.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.ordering.controller.SseController;
import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.OrderStatus;
import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.dto.OrderSaveReqDto;
import com.example.ordersystem.ordering.dto.StockDecreaseEvent;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import com.example.ordersystem.ordering.repository.OrderDetailRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockInventoryService stockInventoryService;
    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;


    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, OrderDetailRepository orderDetailRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler, SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
    }

    /* 주문하기 */
    // @Synchronized 를 설정한다 하더라도, 재고 감소가 DB 에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점이라 싱크가 맞지 않는다.
    public Ordering orderCreate(List<OrderSaveReqDto> dtos) {

        // 방법 2. JPA 최적화 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("회 원 없 음 ."));
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("회원이 존재하지 않습니다."));
        Ordering ordering = Ordering.builder()
                .member(member)
//                .orderDetails() > 값 세팅 수가 없음
                .build();
        for (OrderSaveReqDto orderDto : dtos) {
            Product product = productRepository.findById(orderDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
            int quantity = orderDto.getProductCount();
            // redis 를 통한 재고 관리 + 남은 양
            if(product.getName().contains("sale")){
                // 판매 중이면 redis 재고 확인
                int newQuantity = stockInventoryService.decreaseStock(orderDto.getProductId(), orderDto.getProductCount()).intValue();
                if(newQuantity < 0){
                    throw new IllegalArgumentException("재고가 부족합니다.");
                }
                // rdb(relation db) 에 재고를 업데이트. rabbitmq 를 통해 비동기적으로 이벤트 처리.
                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), orderDto.getProductCount()));

            }
            else{
                if(product.getStockQuantity() < quantity){
                    throw new IllegalArgumentException("재고가 부족합니다. 주문량을 확인해주세요.");
                }
                product.updateStockQuantity(quantity); // 더티체킹
            }
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(quantity)
                    .ordering(ordering)
                    .build();
            // 방법 1 과의 차이점. Repository 에 save 하는 게 아님.
            // save 를 뒤에서 해줘도 JPA 에서 알아서 먼저 실행시켜준다.
            ordering.getOrderDetails().add(orderDetail);
        }
        Ordering savedOrder = orderingRepository.save(ordering);

        sseController.publishMessage(savedOrder.fromEntityList(), "admin@test.com");
        return savedOrder;
    }


    /* 전체 리스트 */
    public List<OrderListResDto> orderList(){
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for(Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntityList());
        }
        return orderListResDtos;
    }

    /* 내 주문 보기 */
    public List<OrderListResDto> myOrders(){
        Member member =memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("Member not found"));
        List<Ordering> orderings = orderingRepository.findByMember(member);
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for(Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntityList());
        }
        return orderListResDtos;
    }

    /* 주문 취소 (admin 기준) */
    public Ordering orderCancel(Long id) {
        Ordering ordering = orderingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ordering not found"));
        ordering.updateStatus(OrderStatus.CANCELLED);
        return ordering;
    }
}
