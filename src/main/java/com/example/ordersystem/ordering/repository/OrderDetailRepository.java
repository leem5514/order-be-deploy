package com.example.ordersystem.ordering.repository;

import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    boolean existsByProduct(Product product);
}
