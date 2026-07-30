package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.ordering.repository.OrderDetailRepository;
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

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProductService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;

    public ProductService(ProductRepository productRepository, OrderDetailRepository orderDetailRepository, S3Client s3Client, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.s3Client = s3Client;
        this.stockInventoryService = stockInventoryService;
    }

    // 상품 등록 : 이미지를 로컬 디스크에 거치지 않고 바로 S3로 업로드한다.
    // AWS_ACCESS_KEY / AWS_SECRET_KEY / AWS_S3_BUCKET 환경변수만 채워주면 별도 코드 수정 없이 동작한다.
    public Product productCreate(ProductSaveDto dto) {
        MultipartFile image = dto.getProductImage();
        Product product = productRepository.save(dto.toEntity());
        try {
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            String s3Path = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImagePath(s3Path);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패 !"); // 트랜잭션 처리를 위해 예외 잡아주기
        }

        // 모든 상품의 재고를 Redis에도 시딩해서, 주문 시 원자적 차감(StockInventoryService)이 가능하게 한다.
        stockInventoryService.increaseStock(product.getId(), dto.getStockQuantity());
        return product;
    }

    @Transactional(readOnly = true)
    public Page<ProductListResDto> productList(ProductSearchDto searchDto, Pageable pageable) {
        // 검색을 위한 Specification 객체 사용
        Specification<Product> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (searchDto.getSearchName() != null && !searchDto.getSearchName().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + searchDto.getSearchName() + "%"));
            }
            if (searchDto.getCategory() != null && !searchDto.getCategory().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("category"), "%" + searchDto.getCategory() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productListDtos = productRepository.findAll(specification, pageable);
        return productListDtos.map(Product::listFromEntity);
    }

    public void productDelete(Long id) {
        // 행 잠금을 걸고 조회 → 이 상품으로 동시에 들어오는 주문(orderCreate도 동일한 잠금 조회를 씀)과
        // 순서가 반드시 직렬화되어, "체크할 땐 주문 없었는데 삭제하는 사이 주문이 들어옴" 레이스를 막는다.
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
        if (orderDetailRepository.existsByProduct(product)) {
            throw new IllegalArgumentException("이미 주문 이력이 있는 상품은 삭제할 수 없습니다.");
        }
        productRepository.delete(product);
        stockInventoryService.removeStock(id);
    }

}
