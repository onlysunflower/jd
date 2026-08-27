package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.ProductReviewDetail;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.service.ProductService;
import com.jdclone.mall.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<List<Product>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(productService.listForUser(keyword, categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.publicDetail(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<ProductReviewDetail>> reviews(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.listByProduct(id));
    }
}
