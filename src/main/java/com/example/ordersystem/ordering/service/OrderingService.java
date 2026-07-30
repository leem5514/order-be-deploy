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
import java.util.Comparator;
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
    // 재고 차감은 상품 종류와 상관없이 전부 Redis 원자 스크립트(StockInventoryService.decreaseStock)로 검증한다.
    // RDB의 stock_quantity는 이 검증이 끝난 뒤 RabbitMQ 이벤트로 비동기 반영되는 "따라가는" 값이다.
    public Ordering orderCreate(List<OrderSaveReqDto> dtos) {

        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("회원이 존재하지 않습니다."));
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        // 주문 저장이 실제로 성공한 뒤에만 RabbitMQ 이벤트를 발행한다 (뒤 상품에서 재고 부족으로
        // 주문 전체가 실패할 경우 이미 발행된 이벤트 때문에 RDB 재고만 먼저 깎이는 걸 막기 위함).
        List<StockDecreaseEvent> pendingStockDecreaseEvents = new ArrayList<>();
        // 이번 주문 처리 중 Redis에서 이미 차감에 성공한 항목들 — 이후 어떤 이유로든(재고 부족,
        // DB 저장 실패, 삭제와의 레이스 등) 주문 전체가 실패하면 여기 담긴 항목들을 복구(보상)해야 한다.
        List<StockDecreaseEvent> appliedRedisDecrements = new ArrayList<>();

        // 상품 ID 오름차순으로 정렬한 뒤 잠금을 건다. 여러 상품을 한 번에 주문할 때 서로 다른 순서로
        // 잠그면 두 트랜잭션이 서로를 기다리는 데드락이 날 수 있어서, 항상 같은 순서로 잠그도록 고정한다.
        List<OrderSaveReqDto> sortedDtos = new ArrayList<>(dtos);
        sortedDtos.sort(Comparator.comparing(OrderSaveReqDto::getProductId));

        try {
            for (OrderSaveReqDto orderDto : sortedDtos) {
                // 행 잠금을 걸고 조회 — 이 상품을 지금 삭제 중인 관리자 트랜잭션이 있다면 그게 끝날 때까지
                // 기다렸다가, 삭제됐으면 "상품이 존재하지 않습니다"로, 아직 있으면 정상 처리로 결과가 갈린다.
                Product product = productRepository.findByIdForUpdate(orderDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
                int quantity = orderDto.getProductCount();

                // GET-검증-DECRBY를 하나의 Lua 스크립트로 묶어 원자적으로 실행 (동시 주문 시 TOCTOU 경합 방지)
                long remaining = stockInventoryService.decreaseStock(product.getId(), quantity, product.getStockQuantity());
                if (remaining < 0) {
                    throw new IllegalArgumentException("재고가 부족합니다.");
                }
                StockDecreaseEvent event = new StockDecreaseEvent(product.getId(), quantity);
                appliedRedisDecrements.add(event);
                pendingStockDecreaseEvents.add(event);

                OrderDetail orderDetail = OrderDetail.builder()
                        .product(product)
                        .quantity(quantity)
                        .ordering(ordering)
                        .build();
                ordering.getOrderDetails().add(orderDetail);
            }

            Ordering savedOrder = orderingRepository.save(ordering);

            pendingStockDecreaseEvents.forEach(stockDecreaseEventHandler::publish);
            sseController.publishMessage(savedOrder.fromEntityList(), "admin@test.com", "ordered");
            return savedOrder;
        } catch (RuntimeException e) {
            // 재고 검증 실패든, DB 저장 시점의 제약조건 위반이든, 여기까지 오는 어떤 실패든
            // 이미 Redis에서 차감된 항목은 전부 복구하고 원래 예외를 그대로 던져 트랜잭션을 롤백시킨다.
            appliedRedisDecrements.forEach(ev -> stockInventoryService.increaseStock(ev.getProductId(), ev.getProductCount()));
            throw e;
        }
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

    /* 주문 취소 (admin 기준) : 취소된 수량만큼 재고를 되돌린다 */
    public Ordering orderCancel(Long id) {
        Ordering ordering = orderingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ordering not found"));
        if (ordering.getOrderstatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("이미 취소된 주문입니다.");
        }
        ordering.updateStatus(OrderStatus.CANCELLED);

        for (OrderDetail detail : ordering.getOrderDetails()) {
            Long productId = detail.getProduct().getId();
            int quantity = detail.getQuantity();
            // Redis는 즉시 원복 (다음 주문의 재고 검증 기준이 바로 이 값이므로 동기 처리)
            stockInventoryService.increaseStock(productId, quantity);
            // RDB는 주문 생성 때와 동일한 큐/리스너(단일 컨슈머라 항상 순서대로 처리됨)를 재사용해서
            // 비동기로 원복한다. 음수 수량 = 차감(-)의 역, 즉 복구를 의미한다.
            stockDecreaseEventHandler.publish(new StockDecreaseEvent(productId, -quantity));
        }

        // 취소당한 본인(구매자)에게 알림. DTO는 아직 세션이 열려있는 지금(트랜잭션 안)에서 미리 만들어서
        // 넘긴다 — 실제 전송은 비동기 스레드에서 이뤄지므로, 그쪽에서 지연로딩 엔티티를 건드리면 안 된다.
        OrderListResDto dto = ordering.fromEntityList();
        sseController.publishMessage(dto, dto.getMemberEmail(), "order-cancelled");
        return ordering;
    }
}
