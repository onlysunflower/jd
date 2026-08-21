package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.CartAddRequest;
import com.jdclone.mall.entity.CartItem;
import com.jdclone.mall.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart/items")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartItem>> list() {
        return ApiResponse.ok(cartService.list());
    }

    @PostMapping
    public ApiResponse<CartItem> add(@Valid @RequestBody CartAddRequest request) {
        return ApiResponse.ok(cartService.add(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CartItem> update(@PathVariable Long id, @RequestParam Integer quantity) {
        return ApiResponse.ok(cartService.updateQuantity(id, quantity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return ApiResponse.ok();
    }
}
