package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.LoginRequest;
import com.jdclone.mall.dto.LoginResponse;
import com.jdclone.mall.dto.RegisterRequest;
import com.jdclone.mall.entity.Merchant;
import com.jdclone.mall.entity.User;
import com.jdclone.mall.mapper.MerchantMapper;
import com.jdclone.mall.mapper.UserMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.TokenService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final TokenService tokenService;

    public AuthService(UserMapper userMapper, MerchantMapper merchantMapper, TokenService tokenService) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.tokenService = tokenService;
    }

    public User register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setPhone(request.getPhone());
        user.setRole(Constants.ROLE_USER);
        user.setStatus("NORMAL");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BizException(403, "账号不可用");
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, user.getId()));
        if (Constants.ROLE_MERCHANT.equals(user.getRole())
                && (merchant == null || !Constants.MERCHANT_APPROVED.equals(merchant.getStatus()))) {
            throw new BizException(403, "商家账号未通过审核或已被冻结");
        }
        Long merchantId = merchant == null ? null : merchant.getId();
        String token = tokenService.create(new AuthUser(user.getId(), user.getUsername(), user.getRole(), merchantId));
        user.setPassword(null);
        return new LoginResponse(token, user, merchantId);
    }
}
