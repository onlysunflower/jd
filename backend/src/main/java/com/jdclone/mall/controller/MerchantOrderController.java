package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.ShipRequest;
import com.jdclone.mall.entity.OrderInfo;
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
@RequestMapping("/api/merchant/orders")
public class MerchantOrderController {
    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderInfo>> list() {
        return ApiResponse.ok(orderService.merchantOrders());
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<OrderInfo> ship(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        return ApiResponse.ok(orderService.ship(id, request));
    }
}
