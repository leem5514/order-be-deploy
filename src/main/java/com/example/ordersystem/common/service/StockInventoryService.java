package com.example.ordersystem.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockInventoryService {
    @Qualifier("3")
    private final RedisTemplate<String, Object> redisTemplate;

    public StockInventoryService(@Qualifier("3") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 상품 등록 시 increaseStock 호출
    public Long increaseStock(Long itemId, int quantity){
        // redis 가 음수까지 내려갈 경우, 추후 재고 업데이트 시 재고와 개수가 정확하지 않을 수 있다.
        // ex) -2 상태에서 재고 100 추가 시 재고가 98개가 됨.
        // 따라서 음수일 경우 0으로 setting 하는 로직이 필요하다.


        // increment 후 재고량을 return 함.
        return redisTemplate.opsForValue().increment(String.valueOf(itemId), quantity);
    }

    // 주문 등록 시 decreaseStock 호출
    public Long decreaseStock(Long itemId, int quantity){
        Object remains = redisTemplate.opsForValue().get(String.valueOf(itemId));
        int longRemains = Integer.parseInt(remains.toString());
        if(longRemains < quantity){
            return -1L;
        }
        else{
            // 감소(decrement) 후 남아있는 잔량 return 됨.
            return redisTemplate.opsForValue().decrement(String.valueOf(itemId), quantity);
        }
    }

}