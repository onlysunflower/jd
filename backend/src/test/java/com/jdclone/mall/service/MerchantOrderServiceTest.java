package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ShipRequest;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.AddressMapper;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceTest {
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private AddressMapper addressMapper;
    @Mock private OperationLogService logService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void merchantOrdersAreScopedAndItemsAreFilled() {
        loginAsMerchant(1L, 50L);
        OrderInfo order = order(20L, 50L, Constants.ORDER_WAIT_SHIP);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(20L);
        item.setProductName("手机");
        item.setPrice(java.math.BigDecimal.valueOf(99));
        item.setQuantity(2);
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        List<OrderInfo> orders = orderService().merchantOrders();

        assertEquals(1, orders.size());
        assertEquals(1, orders.get(0).getItems().size());
        assertEquals("手机", orders.get(0).getItems().get(0).getProductName());
    }

    @Test
    void shipMarksOrderAsWaitingForReceiptAndRecordsLogistics() {
        loginAsMerchant(1L, 50L);
        OrderInfo order = order(21L, 50L, Constants.ORDER_WAIT_SHIP);
        when(orderInfoMapper.selectById(21L)).thenReturn(order);

        ShipRequest request = new ShipRequest();
        request.setLogisticsCompany("京东快递");
        request.setLogisticsNo("JDV1234567890");
        OrderInfo shipped = orderService().ship(21L, request);

        assertEquals(Constants.ORDER_WAIT_RECEIVE, shipped.getStatus());
        assertEquals("京东快递", shipped.getLogisticsCompany());
        assertEquals("JDV1234567890", shipped.getLogisticsNo());
        assertTrue(shipped.getShippedAt() != null);
        verify(orderInfoMapper).updateById(any(OrderInfo.class));
    }

    @Test
    void shipRejectsOrderNotWaitingForShipment() {
        loginAsMerchant(1L, 50L);
        OrderInfo order = order(22L, 50L, Constants.ORDER_WAIT_PAY);
        when(orderInfoMapper.selectById(22L)).thenReturn(order);

        ShipRequest request = new ShipRequest();
        request.setLogisticsCompany("京东快递");
        request.setLogisticsNo("JDV1");

        assertThrows(BizException.class, () -> orderService().ship(22L, request));
        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    void shipRejectsAnotherMerchantsOrder() {
        loginAsMerchant(1L, 50L);
        OrderInfo foreign = order(23L, 999L, Constants.ORDER_WAIT_SHIP);
        when(orderInfoMapper.selectById(23L)).thenReturn(foreign);

        ShipRequest request = new ShipRequest();
        request.setLogisticsCompany("京东快递");
        request.setLogisticsNo("JDV1");

        assertThrows(BizException.class, () -> orderService().ship(23L, request));
        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    private void loginAsMerchant(Long userId, Long merchantId) {
        AuthContext.set(new AuthUser(userId, "shop-owner", Constants.ROLE_MERCHANT, merchantId));
    }

    private OrderService orderService() {
        return new OrderService(orderInfoMapper, orderItemMapper, productMapper, addressMapper, logService);
    }

    private OrderInfo order(Long id, Long merchantId, String status) {
        OrderInfo o = new OrderInfo();
        o.setId(id);
        o.setMerchantId(merchantId);
        o.setStatus(status);
        o.setOrderNo("JD-" + id);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }
}
