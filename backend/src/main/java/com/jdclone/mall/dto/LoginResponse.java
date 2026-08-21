package com.jdclone.mall.dto;

import com.jdclone.mall.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private User user;
    private Long merchantId;
}
