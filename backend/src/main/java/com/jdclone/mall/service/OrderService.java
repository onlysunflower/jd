package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.OrderCreateRequest;
import com.jdclone.mall.dto.CouponDetail;
import com.jdclone.mall.dto.ShipRequest;
import com.jdclone.mall.entity.Address;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.entity.ProductSku;
import com.jdclone.mall.mapper.AddressMapper;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.mapper.ProductSkuMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final AddressMapper addressMapper;
    private final OperationLogService logService;
    private final CouponService couponService;
    private final SettlementService settlementService;

    public OrderService(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            AddressMapper addressMapper,
            OperationLogService logService
    ) {
        this(orderInfoMapper, orderItemMapper, productMapper, null, addressMapper, logService, null, null);
    }

    @Autowired
    public OrderService(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            AddressMapper addressMapper,
            OperationLogService logService,
            CouponService couponService,
            SettlementService settlementService
    ) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.addressMapper = addressMapper;
        this.logService = logService;
        this.couponService = couponService;
        this.settlementService = settlementService;
    }

    public List<OrderInfo> myOrders() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, user.getUserId())
                .orderByDesc(OrderInfo::getCreatedAt));
    }

    public OrderInfo myOrder(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return ownedOrder(id, user.getUserId());
    }

    public List<OrderInfo> merchantOrders() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getCreatedAt);
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            query.eq(OrderInfo::getMerchantId, user.getMerchantId());
        }
        List<OrderInfo> orders = orderInfoMapper.selectList(query);
        for (OrderInfo order : orders) {
            order.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())));
        }
        return orders;
    }

    public List<OrderItem> items(Long orderId) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        ownedOrder(orderId, user.getUserId());
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
        ProductSku sku = resolveSku(product, request.getSkuId());
        BigDecimal unitPrice = sku == null ? product.getPrice() : sku.getPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        CouponDetail coupon = couponService == null ? null
                : couponService.validate(request.getCouponId(), user.getUserId(), total);

        OrderInfo order = new OrderInfo();
        order.setOrderNo("JD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setUserId(user.getUserId());
        order.setMerchantId(product.getMerchantId());
        order.setTotalAmount(total);
        order.setCouponId(coupon == null ? null : coupon.getUserCouponId());
        order.setDiscountAmount(coupon == null ? BigDecimal.ZERO : coupon.getDiscountAmount());
        order.setPayableAmount(total.subtract(order.getDiscountAmount()));
        order.setStatus(Constants.ORDER_WAIT_PAY);
        order.setSettlementStatus("UNSETTLED");
        fillAddress(order, user.getUserId(), request);
        order.setCreatedAt(LocalDateTime.now());
        orderInfoMapper.insert(order);

        lockStock(product, sku, request.getQuantity());
        if (couponService != null) {
            couponService.lock(request.getCouponId(), user.getUserId(), order.getId());
        }

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setSkuId(sku == null ? null : sku.getId());
        item.setSkuCode(sku == null ? null : sku.getSkuCode());
        item.setSpecName(sku == null ? "默认规格" : sku.getSpecName());
        item.setProductName(product.getName());
        item.setProductImage(product.getMainImage());
        item.setPrice(unitPrice);
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
        LocalDateTime paidAt = LocalDateTime.now();
        if (orderInfoMapper.markPaidIfWaiting(order.getId(), paidAt) == 0) {
            throw new BizException("当前订单状态不能支付");
        }
        List<OrderItem> orderItems = items(order.getId());
        if (orderItems.isEmpty()) {
            throw new BizException("订单商品明细不存在，无法支付");
        }
        for (OrderItem item : orderItems) {
            int changed;
            if (item.getSkuId() == null || productSkuMapper == null) {
                changed = productMapper.deductStockAndIncreaseSales(item.getProductId(), item.getQuantity());
            } else {
                changed = productSkuMapper.consumeLockedStock(item.getSkuId(), item.getQuantity());
                if (changed > 0) {
                    changed = productMapper.deductStockAndIncreaseSales(item.getProductId(), item.getQuantity());
                }
            }
            if (changed == 0) {
                throw new BizException("锁定库存异常，无法完成支付");
            }
        }
        if (couponService != null) {
            couponService.markUsed(order.getId());
        }
        order.setStatus(Constants.ORDER_WAIT_SHIP);
        order.setPaidAt(paidAt);
        order.setPaymentNo("PAY" + order.getId() + paidAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        logService.log("ORDER", "PAY", "模拟支付订单：" + order.getOrderNo());
        return order;
    }

    @Transactional
    public OrderInfo cancel(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = ownedOrder(id, user.getUserId());
        if (!Constants.ORDER_WAIT_PAY.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能取消");
        }
        order.setStatus(Constants.ORDER_CANCELED);
        order.setCloseReason("用户主动取消");
        order.setClosedAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        releaseOrderResources(order);
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

    @Transactional
    public OrderInfo confirm(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = ownedOrder(id, user.getUserId());
        if (!Constants.ORDER_WAIT_RECEIVE.equals(order.getStatus())) {
            throw new BizException("当前订单状态不能确认收货");
        }
        order.setStatus(Constants.ORDER_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        if (settlementService != null) {
            settlementService.createForOrder(order);
        }
        logService.log("ORDER", "CONFIRM", "确认收货：" + order.getOrderNo());
        return order;
    }

    public void markRefunding(OrderInfo order) {
        order.setStatus(Constants.ORDER_REFUNDING);
        orderInfoMapper.updateById(order);
    }

    public void restoreStatus(OrderInfo order, String status) {
        order.setStatus(status);
        orderInfoMapper.updateById(order);
    }

    public void markRefundResult(OrderInfo order, boolean success) {
        markRefundResult(order, success, Constants.ORDER_WAIT_RECEIVE);
    }

    public void markRefundResult(OrderInfo order, boolean success, String fallbackStatus) {
        order.setStatus(success ? Constants.ORDER_REFUNDED : fallbackStatus);
        orderInfoMapper.updateById(order);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredOrders() {
        List<OrderInfo> expired = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, Constants.ORDER_WAIT_PAY)
                .le(OrderInfo::getCreatedAt, LocalDateTime.now().minusMinutes(30)));
        for (OrderInfo order : expired) {
            order.setStatus(Constants.ORDER_CANCELED);
            order.setCloseReason("下单 30 分钟未支付，系统自动关闭");
            order.setClosedAt(LocalDateTime.now());
            orderInfoMapper.updateById(order);
            releaseOrderResources(order);
            logService.log("ORDER", "TIMEOUT_CLOSE", "超时关闭订单：" + order.getOrderNo());
        }
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

    private ProductSku resolveSku(Product product, Long skuId) {
        if (productSkuMapper == null) {
            if (product.getStock() < 1) {
                throw new BizException("商品库存不足");
            }
            return null;
        }
        ProductSku sku = skuId == null
                ? productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId())
                        .eq(ProductSku::getStatus, Constants.SHELF_ON)
                        .orderByAsc(ProductSku::getId).last("LIMIT 1"))
                : productSkuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(product.getId())
                || !Constants.SHELF_ON.equals(sku.getStatus())) {
            throw new BizException("所选 SKU 不可购买");
        }
        return sku;
    }

    private void lockStock(Product product, ProductSku sku, int quantity) {
        int changed = productMapper.lockStock(product.getId(), quantity);
        if (changed > 0 && sku != null) {
            changed = productSkuMapper.lockStock(sku.getId(), quantity);
        }
        if (changed == 0) {
            throw new BizException("商品可用库存不足");
        }
    }

    private void releaseOrderResources(OrderInfo order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : orderItems) {
            if (item.getSkuId() == null || productSkuMapper == null) {
                productMapper.releaseLockedStock(item.getProductId(), item.getQuantity());
            } else {
                productSkuMapper.releaseLockedStock(item.getSkuId(), item.getQuantity());
                productMapper.releaseLockedStock(item.getProductId(), item.getQuantity());
            }
        }
        if (couponService != null) {
            couponService.release(order.getId());
        }
    }
}
