package com.example.ordersystem.product.repository;

import com.example.ordersystem.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    // 상품 삭제와 주문 생성이 같은 상품을 동시에 건드릴 때 레이스가 나지 않도록 행 잠금을 걸고 조회한다.
    // (삭제 쪽 existsByProduct 체크와 주문 쪽 재고 검증이 서로를 못 보고 동시에 통과해버리는 걸 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
