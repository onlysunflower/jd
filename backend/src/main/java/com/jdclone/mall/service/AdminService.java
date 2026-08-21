package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.entity.Merchant;
import com.jdclone.mall.entity.OperationLog;
import com.jdclone.mall.entity.User;
import com.jdclone.mall.mapper.MerchantMapper;
import com.jdclone.mall.mapper.OperationLogMapper;
import com.jdclone.mall.mapper.UserMapper;
import com.jdclone.mall.security.RoleGuard;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService logService;

    public AdminService(UserMapper userMapper, MerchantMapper merchantMapper, OperationLogMapper operationLogMapper, OperationLogService logService) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.operationLogMapper = operationLogMapper;
        this.logService = logService;
    }

    public List<User> users() {
        RoleGuard.requireRole(Constants.ROLE_SUPER_ADMIN);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    public List<Merchant> merchants() {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>().orderByDesc(Merchant::getCreatedAt));
    }

    public Merchant updateMerchantStatus(Long id, String status, String reason) {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Merchant merchant = merchantMapper.selectById(id);
        merchant.setStatus(status);
        merchant.setRejectReason(reason);
        merchantMapper.updateById(merchant);
        logService.log("MERCHANT", "STATUS", "商家状态改为：" + status + "，原因：" + reason);
        return merchant;
    }

    public List<OperationLog> logs() {
        RoleGuard.requireRole(Constants.ROLE_SUPER_ADMIN);
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreatedAt));
    }
}
