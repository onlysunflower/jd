package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.OrderCreateRequest;
import com.jdclone.mall.dto.CartOrderCreateRequest;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderInfo>> list() {
        return ApiResponse.ok(orderService.myOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderInfo> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.myOrder(id));
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<OrderItem>> items(@PathVariable Long id) {
        return ApiResponse.ok(orderService.items(id));
    }

    @PostMapping
    public ApiResponse<OrderInfo> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @PostMapping("/from-cart")
    public ApiResponse<List<OrderInfo>> createFromCart(@Valid @RequestBody CartOrderCreateRequest request) {
        return ApiResponse.ok(orderService.createFromCart(request));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderInfo> pay(@PathVariable Long id) {
        return ApiResponse.ok(orderService.pay(id));
    }

    @PostMapping("/{id}/payment-callback")
    public ApiResponse<OrderInfo> paymentCallback(@PathVariable Long id) {
        return ApiResponse.ok(orderService.pay(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderInfo> cancel(@PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderInfo> confirm(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(id));
    }
}
