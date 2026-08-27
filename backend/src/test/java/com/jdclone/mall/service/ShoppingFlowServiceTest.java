package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.CartAddRequest;
import com.jdclone.mall.entity.CartItem;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.AddressMapper;
import com.jdclone.mall.mapper.CartItemMapper;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingFlowServiceTest {
    @Mock private CartItemMapper cartItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private AddressMapper addressMapper;
    @Mock private OperationLogService logService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void addRejectsQuantityThatMakesCartExceedCurrentStock() {
        loginAsUser(1L);
        Product product = purchasableProduct(10L, 5);
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setQuantity(4);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(cartItemMapper.selectOne(any())).thenReturn(existing);

        CartAddRequest request = new CartAddRequest();
        request.setProductId(10L);
        request.setQuantity(2);

        assertThrows(BizException.class, () -> new CartService(cartItemMapper, productMapper).add(request));
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void orderDetailRejectsAnotherUsersOrder() {
        loginAsUser(1L);
        OrderInfo otherUsersOrder = order(20L, 2L, Constants.ORDER_WAIT_PAY);
        when(orderInfoMapper.selectById(20L)).thenReturn(otherUsersOrder);

        assertThrows(BizException.class, () -> orderService().myOrder(20L));
    }

    @Test
    void payRejectsDuplicatePaymentWhenConditionalStatusUpdateFails() {
        loginAsUser(1L);
        OrderInfo order = order(30L, 1L, Constants.ORDER_WAIT_PAY);
        when(orderInfoMapper.selectById(30L)).thenReturn(order);
        when(orderInfoMapper.markPaidIfWaiting(anyLong(), any())).thenReturn(0);

        assertThrows(BizException.class, () -> orderService().pay(30L));
        verify(productMapper, never()).deductStockAndIncreaseSales(anyLong(), any());
    }

    @Test
    void payWithInsufficientStockDoesNotCompleteOrderUpdate() {
        loginAsUser(1L);
        OrderInfo order = order(40L, 1L, Constants.ORDER_WAIT_PAY);
        OrderItem item = new OrderItem();
        item.setOrderId(40L);
        item.setProductId(10L);
        item.setQuantity(2);
        when(orderInfoMapper.selectById(40L)).thenReturn(order);
        when(orderInfoMapper.markPaidIfWaiting(anyLong(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.deductStockAndIncreaseSales(10L, 2)).thenReturn(0);

        assertThrows(BizException.class, () -> orderService().pay(40L));
        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    void confirmRejectsOrderThatIsNotWaitingForReceipt() {
        loginAsUser(1L);
        OrderInfo order = order(50L, 1L, Constants.ORDER_WAIT_SHIP);
        when(orderInfoMapper.selectById(50L)).thenReturn(order);

        assertThrows(BizException.class, () -> orderService().confirm(50L));
        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    private OrderService orderService() {
        return new OrderService(orderInfoMapper, orderItemMapper, productMapper, addressMapper, logService);
    }

    private void loginAsUser(Long userId) {
        AuthContext.set(new AuthUser(userId, "test-user", Constants.ROLE_USER, null));
    }

    private Product purchasableProduct(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setStock(stock);
        product.setPrice(BigDecimal.TEN);
        product.setAuditStatus(Constants.PRODUCT_APPROVED);
        product.setShelfStatus(Constants.SHELF_ON);
        return product;
    }

    private OrderInfo order(Long id, Long userId, String status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setOrderNo("JD-TEST-" + id);
        return order;
    }
}
