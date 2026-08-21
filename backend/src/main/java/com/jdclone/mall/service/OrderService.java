package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.OrderCreateRequest;
import com.jdclone.mall.dto.ShipRequest;
import com.jdclone.mall.entity.Address;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.AddressMapper;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final OperationLogService logService;

    public OrderService(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            AddressMapper addressMapper,
            OperationLogService logService
    ) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.addressMapper = addressMapper;
        this.logService = logService;
    }

    public List<OrderInfo> myOrders() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, user.getUserId())
                .orderByDesc(OrderInfo::getCreatedAt));
    }

    public List<OrderInfo> merchantOrders() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getCreatedAt);
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            query.eq(OrderInfo::getMerchantId, user.getMerchantId());
        }
        return orderInfoMapper.selectList(query);
    }

    public List<OrderItem> items(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    @Transactional
    public OrderInfo create(OrderCreateRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || !Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())
                || !Constants.SHELF_ON.equals(product.getShelfStatus())) {
            throw new BizException("商品不可购买");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new BizException("商品库存不足");
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo("JD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setUserId(user.getUserId());
        order.setMerchantId(product.getMerchantId());
        order.setTotalAmount(product.getPrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity())));
        order.setStatus(Constants.ORDER_WAIT_PAY);
        fillAddress(order, user.getUserId(), request);
        order.setCreatedAt(LocalDateTime.now());
        orderInfoMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getMainImage());
        item.setPrice(product.getPrice());
        item.setQuantity(request.getQuantity());
        orderItemMapper.insert(item);
        logService.log("ORDER", "CREATE", "用户提交订单：" + order.getOrderNo());
        return order;
    }

    @Transactional
    public OrderInfo pay(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = ownedOrder(id, user.getUserId());
        if (!Constants.ORDER_WAIT_PAY.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能支付");
        }
        OrderItem item = items(order.getId()).get(0);
        Product product = productMapper.selectById(item.getProductId());
        if (product.getStock() < item.getQuantity()) {
            throw new BizException("商品库存不足，无法支付");
        }
        product.setStock(product.getStock() - item.getQuantity());
        product.setSales(product.getSales() + item.getQuantity());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        order.setStatus(Constants.ORDER_WAIT_SHIP);
        order.setPaidAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        logService.log("ORDER", "PAY", "模拟支付订单：" + order.getOrderNo());
        return order;
    }

    public OrderInfo cancel(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER, Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (Constants.ROLE_USER.equals(user.getRole()) && !order.getUserId().equals(user.getUserId())) {
            throw new BizException(403, "不能取消其他用户的订单");
        }
        if (!Constants.ORDER_WAIT_PAY.equals(order.getStatus()) && !Constants.ORDER_WAIT_SHIP.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能取消");
        }
        order.setStatus(Constants.ORDER_CANCELED);
        orderInfoMapper.updateById(order);
        logService.log("ORDER", "CANCEL", "取消订单：" + order.getOrderNo());
        return order;
    }

    public OrderInfo ship(Long id, ShipRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (Constants.ROLE_MERCHANT.equals(user.getRole()) && !order.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能处理其他商家的订单");
        }
        if (!Constants.ORDER_WAIT_SHIP.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能发货");
        }
        order.setStatus(Constants.ORDER_WAIT_RECEIVE);
        order.setLogisticsCompany(request.getLogisticsCompany());
        order.setLogisticsNo(request.getLogisticsNo());
        order.setShippedAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        logService.log("ORDER", "SHIP", "商家发货：" + order.getOrderNo());
        return order;
    }

    public OrderInfo confirm(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = ownedOrder(id, user.getUserId());
        if (!Constants.ORDER_WAIT_RECEIVE.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能确认收货");
        }
        order.setStatus(Constants.ORDER_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        logService.log("ORDER", "CONFIRM", "确认收货：" + order.getOrderNo());
        return order;
    }

    public void markRefunding(OrderInfo order) {
        order.setStatus(Constants.ORDER_REFUNDING);
        orderInfoMapper.updateById(order);
    }

    public void markRefundResult(OrderInfo order, boolean success) {
        order.setStatus(success ? Constants.ORDER_REFUNDED : Constants.ORDER_WAIT_RECEIVE);
        orderInfoMapper.updateById(order);
    }

    private OrderInfo ownedOrder(Long id, Long userId) {
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private void fillAddress(OrderInfo order, Long userId, OrderCreateRequest request) {
        if (request.getAddressId() != null) {
            Address address = addressMapper.selectById(request.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BizException("收货地址不存在");
            }
            order.setReceiver(address.getReceiver());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDetail());
            return;
        }
        order.setReceiver(request.getReceiver() == null ? "演示用户" : request.getReceiver());
        order.setReceiverPhone(request.getReceiverPhone() == null ? "13800000000" : request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress() == null ? "北京市朝阳区演示地址" : request.getReceiverAddress());
    }
}
