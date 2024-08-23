package com.example.ordersystem.product.domain;

import com.example.ordersystem.common.domain.Address;
import com.example.ordersystem.member.domain.Role;
import com.example.ordersystem.product.dto.ProductListResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
//@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    public void updateImagePath(String imagePath) {
        this.imagePath = imagePath;
    }


    @Builder
    public Product(Long id, String name, String category, Integer price, Integer stockQuantity, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imagePath = imagePath;
    }

    public ProductListResDto listFromEntity() {
        return ProductListResDto.builder().id(this.id).name(this.name).category(this.category).price(this.price).stockQuantity(this.stockQuantity).imagePath(imagePath).build();
    }

    public void updateStockQuantity(int stockQuantity){
        this.stockQuantity -= stockQuantity;
    }
}
