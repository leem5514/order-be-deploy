package com.example.ordersystem.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class StockInventoryService {
    @Qualifier("3")
    private final RedisTemplate<String, Object> redisTemplate;

    // KEYS[1] = 상품 재고 키, ARGV[1] = 차감할 수량, ARGV[2] = 키가 아직 없을 때 시딩할 초기값(RDB 현재 재고)
    // GET → 검증 → DECRBY 를 하나의 스크립트로 묶어 Redis 서버에서 원자적으로 실행한다.
    // (Java 쪽에서 GET 하고 나중에 DECRBY 하는 방식은 두 요청이 동시에 같은 값을 읽고 둘 다 통과해버리는
    //  TOCTOU(check-then-act) 경합이 생겨서 재고가 음수로 내려갈 수 있다 — 그걸 막기 위한 구조.)
    private static final byte[] DECREASE_STOCK_SCRIPT = (
            "local stock = redis.call('GET', KEYS[1]) " +
            "if stock == false then " +
            "  redis.call('SET', KEYS[1], ARGV[2]) " +
            "  stock = ARGV[2] " +
            "end " +
            "stock = tonumber(stock) " +
            "local qty = tonumber(ARGV[1]) " +
            "if stock >= qty then " +
            "  return redis.call('DECRBY', KEYS[1], qty) " +
            "else " +
            "  return -1 " +
            "end"
    ).getBytes(StandardCharsets.UTF_8);

    public StockInventoryService(@Qualifier("3") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 상품 등록 시 increaseStock 호출
    public Long increaseStock(Long itemId, int quantity){
        // increment 후 재고량을 return 함.
        return redisTemplate.opsForValue().increment(String.valueOf(itemId), quantity);
    }

    // 주문 등록 시 decreaseStock 호출. fallbackStockFromRdb 는 아직 Redis에 재고 키가 없는 경우
    // (레거시 상품, 컨테이너 재기동으로 캐시가 날아간 경우 등) 최초 1회 시딩할 값으로 RDB 현재 재고를 넘긴다.
    // 반환값이 -1이면 재고 부족.
    public Long decreaseStock(Long itemId, int quantity, int fallbackStockFromRdb){
        byte[] keyBytes = String.valueOf(itemId).getBytes(StandardCharsets.UTF_8);
        byte[] quantityBytes = String.valueOf(quantity).getBytes(StandardCharsets.UTF_8);
        byte[] fallbackBytes = String.valueOf(fallbackStockFromRdb).getBytes(StandardCharsets.UTF_8);

        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.eval(DECREASE_STOCK_SCRIPT, ReturnType.INTEGER, 1, keyBytes, quantityBytes, fallbackBytes)
        );
    }

    // 상품 삭제 시 재고 캐시도 같이 정리
    public void removeStock(Long itemId) {
        redisTemplate.delete(String.valueOf(itemId));
    }

}
