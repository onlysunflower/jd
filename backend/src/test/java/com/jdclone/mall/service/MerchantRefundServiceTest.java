package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.RefundRequest;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.RefundLogMapper;
import com.jdclone.mall.mapper.RefundRequestMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantRefundServiceTest {
    @Mock private RefundRequestMapper refundRequestMapper;
    @Mock private RefundLogMapper refundLogMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderService orderService;
    @Mock private OperationLogService logService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void approveMovesReviewingRefundToWaitingUserReturn() {
        loginAsMerchant(1L, 50L);
        RefundRequest refund = refund(30L, 50L, Constants.REFUND_REVIEWING);
        when(refundRequestMapper.selectById(30L)).thenReturn(refund);

        RefundRequest result = refundService().approve(30L, "同意，请寄回");

        assertEquals(Constants.REFUND_WAIT_RETURN, result.getStatus());
        assertEquals("同意，请寄回", result.getMerchantReply());
        verify(refundRequestMapper).updateById(any(RefundRequest.class));
    }

    @Test
    void approveRejectsNonReviewingRefund() {
        loginAsMerchant(1L, 50L);
        RefundRequest refund = refund(31L, 50L, Constants.REFUND_WAIT_RECEIVE);
        when(refundRequestMapper.selectById(31L)).thenReturn(refund);

        assertThrows(BizException.class, () -> refundService().approve(31L, "ok"));
        verify(refundRequestMapper, never()).updateById(any(RefundRequest.class));
    }

    @Test
    void approveRejectsAnotherMerchantsRefund() {
        loginAsMerchant(1L, 50L);
        RefundRequest foreign = refund(32L, 999L, Constants.REFUND_REVIEWING);
        when(refundRequestMapper.selectById(32L)).thenReturn(foreign);

        assertThrows(BizException.class, () -> refundService().approve(32L, "ok"));
        verify(refundRequestMapper, never()).updateById(any(RefundRequest.class));
    }

    @Test
    void rejectMarksRefundAsMerchantRejected() {
        loginAsMerchant(1L, 50L);
        RefundRequest refund = refund(33L, 50L, Constants.REFUND_REVIEWING);
        when(refundRequestMapper.selectById(33L)).thenReturn(refund);

        RefundRequest result = refundService().reject(33L, "已发货不支持");

        assertEquals(Constants.REFUND_REJECTED, result.getStatus());
        assertEquals("已发货不支持", result.getMerchantReply());
    }

    @Test
    void confirmReturnOnlyAllowedWhenWaitingForMerchantReceive() {
        loginAsMerchant(1L, 50L);
        RefundRequest refund = refund(34L, 50L, Constants.REFUND_WAIT_RECEIVE);
        refund.setOrderId(100L);
        OrderInfo order = new OrderInfo();
        order.setId(100L);
        when(refundRequestMapper.selectById(34L)).thenReturn(refund);
        when(orderInfoMapper.selectById(100L)).thenReturn(order);

        RefundRequest result = refundService().confirmReturn(34L, "收到退货，退款");

        assertEquals(Constants.REFUND_SUCCESS, result.getStatus());
        verify(orderService).markRefundResult(order, true);
    }

    @Test
    void confirmReturnRejectsWrongState() {
        loginAsMerchant(1L, 50L);
        RefundRequest refund = refund(35L, 50L, Constants.REFUND_REVIEWING);
        when(refundRequestMapper.selectById(35L)).thenReturn(refund);

        assertThrows(BizException.class, () -> refundService().confirmReturn(35L, "ok"));
        verify(orderService, never()).markRefundResult(any(), any(Boolean.class));
    }

    private void loginAsMerchant(Long userId, Long merchantId) {
        AuthContext.set(new AuthUser(userId, "shop-owner", Constants.ROLE_MERCHANT, merchantId));
    }

    private RefundService refundService() {
        return new RefundService(refundRequestMapper, refundLogMapper, orderInfoMapper, orderService, logService);
    }

    private RefundRequest refund(Long id, Long merchantId, String status) {
        RefundRequest r = new RefundRequest();
        r.setId(id);
        r.setMerchantId(merchantId);
        r.setStatus(status);
        r.setAmount(new BigDecimal("100.00"));
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
