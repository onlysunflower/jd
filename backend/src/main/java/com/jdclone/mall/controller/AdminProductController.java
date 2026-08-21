package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.RefundReplyRequest;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<Product>> pending() {
        return ApiResponse.ok(productService.pending());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Product> approve(@PathVariable Long id) {
        return ApiResponse.ok(productService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Product> reject(@PathVariable Long id, @Valid @RequestBody RefundReplyRequest request) {
        return ApiResponse.ok(productService.reject(id, request.getRemark()));
    }
}
