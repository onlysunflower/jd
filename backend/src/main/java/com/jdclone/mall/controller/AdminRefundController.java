package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.ArbitrateRequest;
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
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {
    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/disputes")
    public ApiResponse<List<RefundRequest>> disputes() {
        return ApiResponse.ok(refundService.disputes());
    }

    @PostMapping("/{id}/arbitrate")
    public ApiResponse<RefundRequest> arbitrate(@PathVariable Long id, @Valid @RequestBody ArbitrateRequest request) {
        return ApiResponse.ok(refundService.arbitrate(id, request));
    }
}
