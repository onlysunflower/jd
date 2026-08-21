package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.RefundCreateRequest;
import com.jdclone.mall.dto.ReturnLogisticsRequest;
import com.jdclone.mall.entity.RefundLog;
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
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ApiResponse<List<RefundRequest>> list() {
        return ApiResponse.ok(refundService.myRefunds());
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<RefundLog>> logs(@PathVariable Long id) {
        return ApiResponse.ok(refundService.logs(id));
    }

    @PostMapping
    public ApiResponse<RefundRequest> create(@Valid @RequestBody RefundCreateRequest request) {
        return ApiResponse.ok(refundService.create(request));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<RefundRequest> submitReturn(@PathVariable Long id, @Valid @RequestBody ReturnLogisticsRequest request) {
        return ApiResponse.ok(refundService.submitReturn(id, request));
    }

    @PostMapping("/{id}/intervention")
    public ApiResponse<RefundRequest> intervention(@PathVariable Long id) {
        return ApiResponse.ok(refundService.requestIntervention(id));
    }
}
