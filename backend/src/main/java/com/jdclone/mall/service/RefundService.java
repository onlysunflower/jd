package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ArbitrateRequest;
import com.jdclone.mall.dto.RefundCreateRequest;
import com.jdclone.mall.dto.ReturnLogisticsRequest;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.RefundLog;
import com.jdclone.mall.entity.RefundRequest;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.RefundLogMapper;
import com.jdclone.mall.mapper.RefundRequestMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
    private final RefundRequestMapper refundRequestMapper;
    private final RefundLogMapper refundLogMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderService orderService;
    private final OperationLogService logService;

    public RefundService(
            RefundRequestMapper refundRequestMapper,
            RefundLogMapper refundLogMapper,
            OrderInfoMapper orderInfoMapper,
            OrderService orderService,
            OperationLogService logService
    ) {
        this.refundRequestMapper = refundRequestMapper;
        this.refundLogMapper = refundLogMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderService = orderService;
        this.logService = logService;
    }

    public List<RefundRequest> myRefunds() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return refundRequestMapper.selectList(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getUserId, user.getUserId())
                .orderByDesc(RefundRequest::getUpdatedAt));
    }

    public List<RefundRequest> merchantRefunds() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        LambdaQueryWrapper<RefundRequest> query = new LambdaQueryWrapper<RefundRequest>().orderByDesc(RefundRequest::getUpdatedAt);
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            query.eq(RefundRequest::getMerchantId, user.getMerchantId());
        }
        return refundRequestMapper.selectList(query);
    }

    public List<RefundRequest> disputes() {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return refundRequestMapper.selectList(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getStatus, Constants.REFUND_PLATFORM)
                .orderByDesc(RefundRequest::getUpdatedAt));
    }

    public List<RefundLog> logs(Long refundId) {
        return refundLogMapper.selectList(new LambdaQueryWrapper<RefundLog>()
                .eq(RefundLog::getRefundId, refundId)
                .orderByAsc(RefundLog::getCreatedAt));
    }

    @Transactional
    public RefundRequest create(RefundCreateRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = orderInfoMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(user.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!Constants.ORDER_WAIT_SHIP.equals(order.getStatus()) && !Constants.ORDER_WAIT_RECEIVE.equals(order.getStatus())
                && !Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能申请售后");
        }
        Long exists = refundRequestMapper.selectCount(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getOrderId, order.getId())
                .notIn(RefundRequest::getStatus, Constants.REFUND_FAILED, Constants.REFUND_CLOSED));
        if (exists > 0) {
            throw new BizException("该订单已有进行中的售后单");
        }
        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setUserId(user.getUserId());
        refund.setMerchantId(order.getMerchantId());
        refund.setType(request.getType());
        refund.setReason(request.getReason());
        refund.setEvidenceImages(request.getEvidenceImages());
        refund.setAmount(order.getTotalAmount());
        refund.setStatus(Constants.REFUND_REVIEWING);
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.insert(refund);
        orderService.markRefunding(order);
        addLog(refund.getId(), "CREATE", "用户提交售后申请：" + request.getReason());
        logService.log("REFUND", "CREATE", "创建售后单：" + refund.getId());
        return refund;
    }

    public RefundRequest approve(Long id, String remark) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        RefundRequest refund = merchantOwned(id, user);
        if (!Constants.REFUND_REVIEWING.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能同意");
        }
        refund.setStatus(Constants.REFUND_WAIT_RETURN);
        refund.setMerchantReply(remark);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        addLog(id, "MERCHANT_APPROVE", remark);
        return refund;
    }

    public RefundRequest reject(Long id, String remark) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        RefundRequest refund = merchantOwned(id, user);
        if (!Constants.REFUND_REVIEWING.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能拒绝");
        }
        refund.setStatus(Constants.REFUND_REJECTED);
        refund.setMerchantReply(remark);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        addLog(id, "MERCHANT_REJECT", remark);
        return refund;
    }

    public RefundRequest submitReturn(Long id, ReturnLogisticsRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(user.getUserId())) {
            throw new BizException("售后单不存在");
        }
        if (!Constants.REFUND_WAIT_RETURN.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能填写退货物流");
        }
        refund.setStatus(Constants.REFUND_WAIT_RECEIVE);
        refund.setReturnLogisticsNo(request.getReturnLogisticsNo());
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        addLog(id, "USER_RETURN", "用户填写退货物流：" + request.getReturnLogisticsNo());
        return refund;
    }

    @Transactional
    public RefundRequest confirmReturn(Long id, String remark) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        RefundRequest refund = merchantOwned(id, user);
        if (!Constants.REFUND_WAIT_RECEIVE.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能确认收货");
        }
        refund.setStatus(Constants.REFUND_SUCCESS);
        refund.setMerchantReply(remark);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        orderService.markRefundResult(orderInfoMapper.selectById(refund.getOrderId()), true);
        addLog(id, "MERCHANT_CONFIRM_RETURN", remark);
        return refund;
    }

    public RefundRequest requestIntervention(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(user.getUserId())) {
            throw new BizException("售后单不存在");
        }
        if (!Constants.REFUND_REJECTED.equals(refund.getStatus()) && !Constants.REFUND_REVIEWING.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能申请平台介入");
        }
        refund.setStatus(Constants.REFUND_PLATFORM);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        addLog(id, "USER_REQUEST_INTERVENTION", "用户申请平台客服介入");
        return refund;
    }

    @Transactional
    public RefundRequest arbitrate(Long id, ArbitrateRequest request) {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null) {
            throw new BizException("售后单不存在");
        }
        if (!Constants.REFUND_PLATFORM.equals(refund.getStatus())) {
            throw new BizException("只有平台介入中的售后单可以仲裁");
        }
        boolean approve = "APPROVE".equalsIgnoreCase(request.getDecision());
        refund.setStatus(approve ? Constants.REFUND_SUCCESS : Constants.REFUND_FAILED);
        refund.setAdminDecision(approve ? "同意退款" : "驳回退款");
        refund.setAdminRemark(request.getRemark());
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        orderService.markRefundResult(orderInfoMapper.selectById(refund.getOrderId()), approve);
        addLog(id, "ADMIN_ARBITRATE", refund.getAdminDecision() + "：" + request.getRemark());
        logService.log("REFUND", "ARBITRATE", "管理员仲裁售后单：" + id);
        return refund;
    }

    private RefundRequest merchantOwned(Long id, AuthUser user) {
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null) {
            throw new BizException("售后单不存在");
        }
        if (Constants.ROLE_MERCHANT.equals(user.getRole()) && !refund.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能处理其他商家的售后单");
        }
        return refund;
    }

    private void addLog(Long refundId, String action, String remark) {
        AuthUser user = RoleGuard.requireLogin();
        RefundLog log = new RefundLog();
        log.setRefundId(refundId);
        log.setOperatorId(user.getUserId());
        log.setOperatorRole(user.getRole());
        log.setAction(action);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        refundLogMapper.insert(log);
    }
}
