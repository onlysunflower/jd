package com.jdclone.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.ProductRequest;
import com.jdclone.mall.entity.Category;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.CategoryMapper;
import com.jdclone.mall.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
public class MerchantProductController {
    private final ProductService productService;
    private final CategoryMapper categoryMapper;

    public MerchantProductController(ProductService productService, CategoryMapper categoryMapper) {
        this.productService = productService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder).orderByAsc(Category::getId)));
    }

    @GetMapping("/products")
    public ApiResponse<List<Product>> list() {
        return ApiResponse.ok(productService.listForMerchant());
    }

    @PostMapping("/products")
    public ApiResponse<Product> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @PostMapping("/products/{id}/off-shelf")
    public ApiResponse<Product> offShelf(@PathVariable Long id) {
        return ApiResponse.ok(productService.offShelf(id));
    }

    @PostMapping("/products/{id}/on-shelf")
    public ApiResponse<Product> onShelf(@PathVariable Long id) {
        return ApiResponse.ok(productService.onShelf(id));
    }
}
