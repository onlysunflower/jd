package com.jdclone.mall.service;

import com.jdclone.mall.entity.OperationLog;
import com.jdclone.mall.mapper.OperationLogMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void log(String module, String action, String detail) {
        AuthUser user = AuthContext.get();
        OperationLog log = new OperationLog();
        log.setOperatorId(user == null ? null : user.getUserId());
        log.setOperatorRole(user == null ? "SYSTEM" : user.getRole());
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
