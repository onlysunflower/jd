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
    private final SettlementService settlementService;

    public RefundService(
            RefundRequestMapper refundRequestMapper,
            RefundLogMapper refundLogMapper,
            OrderInfoMapper orderInfoMapper,
            OrderService orderService,
            OperationLogService logService,
            SettlementService settlementService
    ) {
        this.refundRequestMapper = refundRequestMapper;
        this.refundLogMapper = refundLogMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderService = orderService;
        this.logService = logService;
        this.settlementService = settlementService;
    }

    public List<RefundRequest> myRefunds() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return refundRequestMapper.selectList(new LambdaQueryWrapper<RefundRequest>()
                .eq(RefundRequest::getUserId, user.getUserId())
                .orderByDesc(RefundRequest::getUpdatedAt));
    }

    public List<RefundRequest> merchantRefunds() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        LambdaQueryWrapper<RefundRequest> query =
                new LambdaQueryWrapper<RefundRequest>().orderByDesc(RefundRequest::getUpdatedAt);
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
        AuthUser user = RoleGuard.requireLogin();
        RefundRequest refund = refundRequestMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException("售后单不存在");
        }
        ensureCanViewLogs(refund, user);
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
        validateRefundType(request.getType());
        if (!Constants.ORDER_WAIT_SHIP.equals(order.getStatus())
                && !Constants.ORDER_WAIT_RECEIVE.equals(order.getStatus())
                && !Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能申请售后");
        }
        ensureTypeMatchesOrderStatus(order.getStatus(), request.getType());
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
        refund.setSourceOrderStatus(order.getStatus());
        refund.setReason(request.getReason());
        refund.setEvidenceImages(request.getEvidenceImages());
        refund.setAmount(order.getPayableAmount() == null ? order.getTotalAmount() : order.getPayableAmount());
        refund.setStatus(Constants.REFUND_REVIEWING);
        refund.setOriginalOrderStatus(order.getStatus());
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.insert(refund);
        orderService.markRefunding(order);
        addLog(refund.getId(), "CREATE", "用户提交售后申请：" + request.getReason());
        logService.log("REFUND", "CREATE", "创建售后单：" + refund.getId());
        return refund;
    }

    @Transactional
    public RefundRequest approve(Long id, String remark) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        RefundRequest refund = merchantOwned(id, user);
        if (!Constants.REFUND_REVIEWING.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能同意");
        }

        boolean refundOnly = Constants.REFUND_TYPE_REFUND_ONLY.equals(refund.getType());
        refund.setStatus(refundOnly ? Constants.REFUND_SUCCESS : Constants.REFUND_WAIT_RETURN);
        refund.setMerchantReply(remark);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        if (refundOnly) {
            orderService.markRefundResult(orderInfoMapper.selectById(refund.getOrderId()), true);
        }
        addLog(id, "MERCHANT_APPROVE", remark);
        if (!returnRequired) {
            OrderInfo order = orderInfoMapper.selectById(refund.getOrderId());
            orderService.markRefundResult(order, true);
            settlementService.reverseForOrder(order.getId());
            addLog(id, "REFUND_DIRECT", "未收货或仅退款申请，无需寄回，退款完成");
        }
        return refund;
    }

    @Transactional
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
        restoreOriginalOrderStatus(refund);
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
        settlementService.reverseForOrder(refund.getOrderId());
        addLog(id, "MERCHANT_CONFIRM_RETURN", remark);
        return refund;
    }

    @Transactional
    public RefundRequest requestIntervention(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(user.getUserId())) {
            throw new BizException("售后单不存在");
        }
        if (!Constants.REFUND_REJECTED.equals(refund.getStatus())
                && !Constants.REFUND_REVIEWING.equals(refund.getStatus())) {
            throw new BizException("当前售后状态不能申请平台介入");
        }
        refund.setStatus(Constants.REFUND_PLATFORM);
        refund.setUpdatedAt(LocalDateTime.now());
        refundRequestMapper.updateById(refund);
        orderService.markRefunding(orderInfoMapper.selectById(refund.getOrderId()));
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

        OrderInfo order = orderInfoMapper.selectById(refund.getOrderId());
        if (approve) {
            orderService.markRefundResult(order, true);
        } else {
            orderService.restoreStatus(order, resolveOriginalOrderStatus(refund, order));
        }

        addLog(id, "ADMIN_ARBITRATE", refund.getAdminDecision() + "：" + request.getRemark());
        logService.log("REFUND", "ARBITRATE", "管理员仲裁售后单：" + id);
        return refund;
    }

    private RefundRequest merchantOwned(Long id, AuthUser user) {
        RefundRequest refund = refundRequestMapper.selectById(id);
        if (refund == null) {
            throw new BizException("售后单不存在");
        }
        if (Constants.ROLE_MERCHANT.equals(user.getRole())
                && !refund.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能处理其他商家的售后单");
        }
        return refund;
    }

    private void validateRefundType(String type) {
        if (!Constants.REFUND_TYPE_REFUND_ONLY.equals(type)
                && !Constants.REFUND_TYPE_RETURN_AND_REFUND.equals(type)) {
            throw new BizException("售后类型不合法");
        }
    }

    private void ensureTypeMatchesOrderStatus(String orderStatus, String type) {
        boolean allowed = (Constants.ORDER_WAIT_SHIP.equals(orderStatus)
                && Constants.REFUND_TYPE_REFUND_ONLY.equals(type))
                || (Constants.ORDER_WAIT_RECEIVE.equals(orderStatus)
                && (Constants.REFUND_TYPE_REFUND_ONLY.equals(type)
                || Constants.REFUND_TYPE_RETURN_AND_REFUND.equals(type)))
                || (Constants.ORDER_COMPLETED.equals(orderStatus)
                && Constants.REFUND_TYPE_RETURN_AND_REFUND.equals(type));
        if (!allowed) {
            throw new BizException("当前订单状态不支持该售后类型");
        }
    }

    private void ensureCanViewLogs(RefundRequest refund, AuthUser user) {
        if (Constants.ROLE_USER.equals(user.getRole())) {
            if (!refund.getUserId().equals(user.getUserId())) {
                throw new BizException(403, "不能查看其他用户的售后日志");
            }
            return;
        }
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            if (!refund.getMerchantId().equals(user.getMerchantId())) {
                throw new BizException(403, "不能查看其他商家的售后日志");
            }
            return;
        }
        if (Constants.ROLE_SERVICE_ADMIN.equals(user.getRole())
                || Constants.ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return;
        }
        throw new BizException(403, "当前角色没有查看售后日志的权限");
    }

    private void restoreOriginalOrderStatus(RefundRequest refund) {
        OrderInfo order = orderInfoMapper.selectById(refund.getOrderId());
        orderService.restoreStatus(order, resolveOriginalOrderStatus(refund, order));
    }

    private String resolveOriginalOrderStatus(RefundRequest refund, OrderInfo order) {
        String originalStatus = refund.getOriginalOrderStatus();
        if (Constants.ORDER_WAIT_SHIP.equals(originalStatus)
                || Constants.ORDER_WAIT_RECEIVE.equals(originalStatus)
                || Constants.ORDER_COMPLETED.equals(originalStatus)) {
            return originalStatus;
        }
        if (Constants.ORDER_WAIT_SHIP.equals(order.getStatus())
                || Constants.ORDER_WAIT_RECEIVE.equals(order.getStatus())
                || Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            return order.getStatus();
        }
        if (order.getCompletedAt() != null) {
            return Constants.ORDER_COMPLETED;
        }
        if (order.getShippedAt() != null) {
            return Constants.ORDER_WAIT_RECEIVE;
        }
        if (order.getPaidAt() != null) {
            return Constants.ORDER_WAIT_SHIP;
        }
        return Constants.ORDER_WAIT_RECEIVE;
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
