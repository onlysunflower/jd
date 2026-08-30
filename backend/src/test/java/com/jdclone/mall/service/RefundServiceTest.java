package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
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
    void logsRequireLogin() {
        assertThrows(BizException.class, () -> service().logs(6L));
        verify(refundRequestMapper, never()).selectById(any());
        verify(refundLogMapper, never()).selectList(any());
    }

    @Test
    void logsRejectOtherUser() {
        loginAsUser(1L);
        RefundRequest refund = refund(6L, 30L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_REVIEWING);
        refund.setUserId(9L);
        when(refundRequestMapper.selectById(6L)).thenReturn(refund);

        assertThrows(BizException.class, () -> service().logs(6L));
        verify(refundLogMapper, never()).selectList(any());
    }

    @Test
    void logsAllowMerchantOwner() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(6L, 30L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_REVIEWING);
        RefundLog log = new RefundLog();
        log.setRefundId(6L);
        when(refundRequestMapper.selectById(6L)).thenReturn(refund);
        when(refundLogMapper.selectList(any())).thenReturn(List.of(log));

        List<RefundLog> logs = service().logs(6L);

        assertEquals(1, logs.size());
        assertSame(log, logs.get(0));
    }

    @Test
    void logsAllowServiceAdmin() {
        loginAsServiceAdmin(3L);
        RefundRequest refund = refund(7L, 31L, 99L, Constants.REFUND_TYPE_RETURN_AND_REFUND, Constants.REFUND_PLATFORM);
        when(refundRequestMapper.selectById(7L)).thenReturn(refund);
        when(refundLogMapper.selectList(any())).thenReturn(List.of());

        assertEquals(0, service().logs(7L).size());
    }

    @Test
    void createRejectsUnknownRefundType() {
        loginAsUser(1L);
        when(orderInfoMapper.selectById(10L)).thenReturn(order(10L, 1L, 2L, Constants.ORDER_WAIT_RECEIVE));

        RefundCreateRequest request = new RefundCreateRequest();
        request.setOrderId(10L);
        request.setType("ONLY_MONEY_BACK");
        request.setReason("invalid type");

        assertThrows(BizException.class, () -> service().create(request));
        verify(refundRequestMapper, never()).selectCount(any());
        verify(orderService, never()).markRefunding(any(OrderInfo.class));
    }

    @Test
    void createRejectsReturnAndRefundForWaitShipOrder() {
        loginAsUser(1L);
        when(orderInfoMapper.selectById(11L)).thenReturn(order(11L, 1L, 2L, Constants.ORDER_WAIT_SHIP));

        RefundCreateRequest request = new RefundCreateRequest();
        request.setOrderId(11L);
        request.setType(Constants.REFUND_TYPE_RETURN_AND_REFUND);
        request.setReason("not allowed");

        assertThrows(BizException.class, () -> service().create(request));
        verify(refundRequestMapper, never()).selectCount(any());
        verify(orderService, never()).markRefunding(any(OrderInfo.class));
    }

    @Test
    void createRejectsDuplicateRefundRequestForSameOrder() {
        loginAsUser(1L);
        OrderInfo order = order(13L, 1L, 2L, Constants.ORDER_WAIT_RECEIVE);
        when(orderInfoMapper.selectById(13L)).thenReturn(order);
        when(refundRequestMapper.selectCount(any())).thenReturn(1L);

        RefundCreateRequest request = new RefundCreateRequest();
        request.setOrderId(13L);
        request.setType(Constants.REFUND_TYPE_REFUND_ONLY);
        request.setReason("duplicate");

        assertThrows(BizException.class, () -> service().create(request));
        verify(refundRequestMapper, never()).insert(any(RefundRequest.class));
        verify(orderService, never()).markRefunding(order);
    }

    @Test
    void createStoresOriginalOrderStatus() {
        loginAsUser(1L);
        OrderInfo order = order(12L, 1L, 2L, Constants.ORDER_COMPLETED);
        when(orderInfoMapper.selectById(12L)).thenReturn(order);
        when(refundRequestMapper.selectCount(any())).thenReturn(0L);

        RefundCreateRequest request = new RefundCreateRequest();
        request.setOrderId(12L);
        request.setType(Constants.REFUND_TYPE_RETURN_AND_REFUND);
        request.setReason("quality issue");

        RefundRequest refund = service().create(request);

        assertEquals(Constants.REFUND_REVIEWING, refund.getStatus());
        assertEquals(Constants.ORDER_COMPLETED, refund.getOriginalOrderStatus());
        verify(refundRequestMapper).insert(any(RefundRequest.class));
        verify(orderService).markRefunding(order);
    }

    @Test
    void approveCompletesRefundOnlyImmediately() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(20L, 99L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_REVIEWING);
        OrderInfo order = order(99L, 1L, 2L, Constants.ORDER_REFUNDING);
        when(refundRequestMapper.selectById(20L)).thenReturn(refund);
        when(orderInfoMapper.selectById(99L)).thenReturn(order);

        RefundRequest approved = service().approve(20L, "merchant agreed");

        assertEquals(Constants.REFUND_SUCCESS, approved.getStatus());
        verify(refundRequestMapper).updateById(refund);
        verify(orderService).markRefundResult(order, true);
    }

    @Test
    void approveReturnAndRefundWaitsForUserReturn() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(21L, 100L, 2L, Constants.REFUND_TYPE_RETURN_AND_REFUND, Constants.REFUND_REVIEWING);
        when(refundRequestMapper.selectById(21L)).thenReturn(refund);

        RefundRequest approved = service().approve(21L, "return goods");

        assertEquals(Constants.REFUND_WAIT_RETURN, approved.getStatus());
        verify(orderService, never()).markRefundResult(any(OrderInfo.class), anyBoolean());
    }

    @Test
    void approveRejectsRepeatedMerchantReview() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(30L, 110L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_SUCCESS);
        when(refundRequestMapper.selectById(30L)).thenReturn(refund);

        assertThrows(BizException.class, () -> service().approve(30L, "repeat approve"));
        verify(refundRequestMapper, never()).updateById(any(RefundRequest.class));
        verify(orderService, never()).markRefundResult(any(OrderInfo.class), anyBoolean());
    }

    @Test
    void rejectRestoresWaitShipOrderStatus() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(22L, 101L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_REVIEWING);
        refund.setOriginalOrderStatus(Constants.ORDER_WAIT_SHIP);
        OrderInfo order = order(101L, 1L, 2L, Constants.ORDER_REFUNDING);
        when(refundRequestMapper.selectById(22L)).thenReturn(refund);
        when(orderInfoMapper.selectById(101L)).thenReturn(order);

        RefundRequest rejected = service().reject(22L, "merchant rejected");

        assertEquals(Constants.REFUND_REJECTED, rejected.getStatus());
        verify(orderService).restoreStatus(order, Constants.ORDER_WAIT_SHIP);
    }

    @Test
    void submitReturnRejectsRepeatedLogisticsSubmission() {
        loginAsUser(1L);
        RefundRequest refund = refund(31L, 111L, 2L, Constants.REFUND_TYPE_RETURN_AND_REFUND, Constants.REFUND_WAIT_RECEIVE);
        when(refundRequestMapper.selectById(31L)).thenReturn(refund);

        ReturnLogisticsRequest request = new ReturnLogisticsRequest();
        request.setReturnLogisticsNo("SF123456789");

        assertThrows(BizException.class, () -> service().submitReturn(31L, request));
        verify(refundRequestMapper, never()).updateById(any(RefundRequest.class));
    }

    @Test
    void confirmReturnRejectsRepeatedConfirmation() {
        loginAsMerchant(8L, 2L);
        RefundRequest refund = refund(32L, 112L, 2L, Constants.REFUND_TYPE_RETURN_AND_REFUND, Constants.REFUND_SUCCESS);
        when(refundRequestMapper.selectById(32L)).thenReturn(refund);

        assertThrows(BizException.class, () -> service().confirmReturn(32L, "repeat confirm"));
        verify(refundRequestMapper, never()).updateById(any(RefundRequest.class));
        verify(orderService, never()).markRefundResult(any(OrderInfo.class), anyBoolean());
    }

    @Test
    void arbitrateRejectRestoresCompletedOrderStatus() {
        loginAsServiceAdmin(3L);
        RefundRequest refund = refund(23L, 102L, 2L, Constants.REFUND_TYPE_RETURN_AND_REFUND, Constants.REFUND_PLATFORM);
        refund.setOriginalOrderStatus(Constants.ORDER_COMPLETED);
        OrderInfo order = order(102L, 1L, 2L, Constants.ORDER_REFUNDING);
        when(refundRequestMapper.selectById(23L)).thenReturn(refund);
        when(orderInfoMapper.selectById(102L)).thenReturn(order);

        ArbitrateRequest request = new ArbitrateRequest();
        request.setDecision("REJECT");
        request.setRemark("no refund");

        RefundRequest result = service().arbitrate(23L, request);

        assertEquals(Constants.REFUND_FAILED, result.getStatus());
        verify(orderService).restoreStatus(order, Constants.ORDER_COMPLETED);
    }

    @Test
    void arbitrateRejectRestoresWaitReceiveOrderStatus() {
        loginAsServiceAdmin(3L);
        RefundRequest refund = refund(24L, 103L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_PLATFORM);
        refund.setOriginalOrderStatus(Constants.ORDER_WAIT_RECEIVE);
        OrderInfo order = order(103L, 1L, 2L, Constants.ORDER_REFUNDING);
        when(refundRequestMapper.selectById(24L)).thenReturn(refund);
        when(orderInfoMapper.selectById(103L)).thenReturn(order);

        ArbitrateRequest request = new ArbitrateRequest();
        request.setDecision("REJECT");
        request.setRemark("restore wait receive");

        RefundRequest result = service().arbitrate(24L, request);

        assertEquals(Constants.REFUND_FAILED, result.getStatus());
        verify(orderService).restoreStatus(order, Constants.ORDER_WAIT_RECEIVE);
    }

    @Test
    void arbitrateRejectInfersOriginalStatusFromOrderLifecycleWhenSnapshotMissing() {
        loginAsServiceAdmin(3L);
        RefundRequest refund = refund(25L, 104L, 2L, Constants.REFUND_TYPE_REFUND_ONLY, Constants.REFUND_PLATFORM);
        OrderInfo order = order(104L, 1L, 2L, Constants.ORDER_REFUNDING);
        order.setShippedAt(LocalDateTime.now().minusDays(1));
        when(refundRequestMapper.selectById(25L)).thenReturn(refund);
        when(orderInfoMapper.selectById(104L)).thenReturn(order);

        ArbitrateRequest request = new ArbitrateRequest();
        request.setDecision("REJECT");
        request.setRemark("fallback");

        service().arbitrate(25L, request);

        verify(orderService).restoreStatus(order, Constants.ORDER_WAIT_RECEIVE);
    }

    private RefundService service() {
        return new RefundService(refundRequestMapper, refundLogMapper, orderInfoMapper, orderService, logService);
    }

    private void loginAsUser(Long userId) {
        AuthContext.set(new AuthUser(userId, "test-user", Constants.ROLE_USER, null));
    }

    private void loginAsMerchant(Long userId, Long merchantId) {
        AuthContext.set(new AuthUser(userId, "test-merchant", Constants.ROLE_MERCHANT, merchantId));
    }

    private void loginAsServiceAdmin(Long userId) {
        AuthContext.set(new AuthUser(userId, "service-admin", Constants.ROLE_SERVICE_ADMIN, null));
    }

    private OrderInfo order(Long id, Long userId, Long merchantId, String status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(128.88));
        return order;
    }

    private RefundRequest refund(Long id, Long orderId, Long merchantId, String type, String status) {
        RefundRequest refund = new RefundRequest();
        refund.setId(id);
        refund.setOrderId(orderId);
        refund.setUserId(1L);
        refund.setMerchantId(merchantId);
        refund.setType(type);
        refund.setStatus(status);
        return refund;
    }
}
