package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.entity.Merchant;
import com.jdclone.mall.entity.OperationLog;
import com.jdclone.mall.entity.User;
import com.jdclone.mall.service.AdminService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users() {
        return ApiResponse.ok(adminService.users());
    }

    @GetMapping("/merchants")
    public ApiResponse<List<Merchant>> merchants() {
        return ApiResponse.ok(adminService.merchants());
    }

    @PostMapping("/merchants/{id}/status")
    public ApiResponse<Merchant> merchantStatus(@PathVariable Long id,
                                                @RequestParam String status,
                                                @RequestParam(required = false) String reason) {
        return ApiResponse.ok(adminService.updateMerchantStatus(id, status, reason));
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs() {
        return ApiResponse.ok(adminService.logs());
    }
}
