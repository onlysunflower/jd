package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.RefundReplyRequest;
import com.jdclone.mall.entity.RefundRequest;
import com.jdclone.mall.service.RefundService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/refunds")
public class MerchantRefundController {
    private final RefundService refundService;

    public MerchantRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ApiResponse<List<RefundRequest>> list() {
        return ApiResponse.ok(refundService.merchantRefunds());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<RefundRequest> approve(@PathVariable Long id, @Valid @RequestBody RefundReplyRequest request) {
        return ApiResponse.ok(refundService.approve(id, request.getRemark()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<RefundRequest> reject(@PathVariable Long id, @Valid @RequestBody RefundReplyRequest request) {
        return ApiResponse.ok(refundService.reject(id, request.getRemark()));
    }

    @PostMapping("/{id}/confirm-return")
    public ApiResponse<RefundRequest> confirmReturn(@PathVariable Long id, @Valid @RequestBody RefundReplyRequest request) {
        return ApiResponse.ok(refundService.confirmReturn(id, request.getRemark()));
    }
}
