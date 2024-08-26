package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductListResDto;
import com.example.ordersystem.product.dto.ProductSaveDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final ProductRepository productRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;

    public ProductService(ProductRepository productRepository, S3Client s3Client, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.s3Client = s3Client;
        this.stockInventoryService = stockInventoryService;
    }

    public Product productCreate(ProductSaveDto dto) {
        MultipartFile image = dto.getProductImage();
        Product product = null;
        try {
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();
            Path path = Paths.get("/tmp/", fileName);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            product.updateImagePath(path.toString());

            if(dto.getName().contains("sale")){
                stockInventoryService.increaseStock(product.getId(), dto.getStockQuantity());
            }
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패 !"); // 트랜잭션 처리를 위해 예외 잡아주기
        }
        return product;
    }

    @Transactional
    public Page<ProductListResDto> productList(ProductSearchDto searchDto, Pageable pageable) {
        // 검색을 위한 Specification 객체 사용
        // specification 객체는 복잡한 쿼리를 이용하여 정의하는 방식으로, 쿼리를 쉽게
        Specification<Optional> specification = new Specification<Optional>() {
            @Override
            public Predicate toPredicate(Root<Optional> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if (searchDto.getSearchName() != null) {
                    predicates.add(criteriaBuilder.like(root.get("name"), "%" + searchDto.getSearchName() + "%"));
                }
                if (searchDto.getCategory() != null) {
                    predicates.add(criteriaBuilder.like(root.get("category"), "%" + searchDto.getSearchName() + "%"));
                }

                Predicate[] predicateArr = new Predicate[predicates.size()];
                for (int i = 0; i < predicateArr.length; i++) {
                    predicateArr[i] = predicates.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                return predicate;
            }
        };
        Page<Product> productListDtos = productRepository.findAll(pageable);
        return productListDtos.map(a -> a.listFromEntity());
    }

    @Transactional
    public Product productAwsCreate(ProductSaveDto dto) {
        MultipartFile image = dto.getProductImage();
        Product product = null;
        try {
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();
            Path path = Paths.get("C:\\Users\\Playdata\\Desktop\\tmp", fileName);

            // local pc 에 임시 저장.
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            // aws 에 pc 저장 파일을 업로드.
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromFile(path));
            String s3Path = s3Client.utilities().getUrl(a->a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImagePath(s3Path);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패 !"); // 트랜잭션 처리를 위해 예외 잡아주기
        }
        return product;
    }

}
