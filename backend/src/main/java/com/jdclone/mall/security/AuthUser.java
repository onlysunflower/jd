package com.jdclone.mall.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long userId;
    private String username;
    private String role;
    private Long merchantId;
}
