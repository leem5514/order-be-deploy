package com.example.ordersystem.product.controller;

import com.example.ordersystem.common.dto.CommonResDto;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.MemberListDto;
import com.example.ordersystem.member.dto.MemberSaveDto;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductListResDto;
import com.example.ordersystem.product.dto.ProductSaveDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    //
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/product/create") // 생성
    public ResponseEntity<?> productCreate(ProductSaveDto dto) {
//        Product product = productService.productCreate(dto);
        Product product = productService.productAwsCreate(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "success created", product.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }


    @GetMapping("/product/list")
    public ResponseEntity<?> productList(ProductSearchDto searchDto, Pageable pageable) {
        Page<ProductListResDto> productListDtos = productService.productList(searchDto, pageable);

        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "success linked", productListDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

}

