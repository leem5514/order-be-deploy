package com.example.ordersystem.product.dto;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.domain.Role;
import com.example.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaveDto {
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private MultipartFile productImage;

    public Product toEntity() {
        Product product = Product.builder()
                .name(this.name)
                .category(this.category)
                .price(this.price)
                .stockQuantity(this.stockQuantity)
//                .imagePath(imagePath)
                .build();
        return product;
    }


}
