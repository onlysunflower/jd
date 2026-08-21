package com.jdclone.mall.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class TokenService {
    public String create(AuthUser user) {
        String raw = user.getUserId() + "|" + user.getUsername() + "|" + user.getRole() + "|"
                + (user.getMerchantId() == null ? 0 : user.getMerchantId()) + "|" + System.currentTimeMillis();
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public AuthUser parse(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            Long merchantId = Long.parseLong(parts[3]) == 0 ? null : Long.parseLong(parts[3]);
            return new AuthUser(Long.parseLong(parts[0]), parts[1], parts[2], merchantId);
        } catch (Exception e) {
            return null;
        }
    }
}
