package com.jdclone.mall.security;

import com.jdclone.mall.common.BizException;
import java.util.Arrays;

public final class RoleGuard {
    private RoleGuard() {
    }

    public static AuthUser requireLogin() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new BizException(401, "请先登录");
        }
        return user;
    }

    public static AuthUser requireRole(String... roles) {
        AuthUser user = requireLogin();
        if (Arrays.stream(roles).noneMatch(role -> role.equals(user.getRole()))) {
            throw new BizException(403, "当前角色没有操作权限");
        }
        return user;
    }
}
