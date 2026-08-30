package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ProductReviewDetail;
import com.jdclone.mall.dto.ReviewRequest;
import com.jdclone.mall.dto.ReviewTask;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.OrderItem;
import com.jdclone.mall.entity.Review;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.entity.User;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ReviewMapper;
import com.jdclone.mall.mapper.UserMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OperationLogService logService;

    public ReviewService(ReviewMapper reviewMapper, OrderInfoMapper orderInfoMapper,
                         OrderItemMapper orderItemMapper, UserMapper userMapper) {
        this(reviewMapper, orderInfoMapper, orderItemMapper, userMapper, null, null);
    }

    @Autowired
    public ReviewService(ReviewMapper reviewMapper, OrderInfoMapper orderInfoMapper,
                         OrderItemMapper orderItemMapper, UserMapper userMapper,
                         ProductMapper productMapper, OperationLogService logService) {
        this.reviewMapper = reviewMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.logService = logService;
    }

    @Transactional
    public Review create(ReviewRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = orderInfoMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(user.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new BizException("订单完成后才能评价");
        }
        Long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, request.getOrderId())
                .eq(OrderItem::getProductId, request.getProductId()));
        if (itemCount == 0) {
            throw new BizException("该商品不属于当前订单");
        }
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId())
                .eq(Review::getProductId, request.getProductId()));
        if (count > 0) {
            throw new BizException("不能重复评价");
        }
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setProductId(request.getProductId());
        review.setUserId(user.getUserId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        review.setStatus("VISIBLE");
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        return review;
    }

    public List<ProductReviewDetail> listByProduct(Long productId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .and(wrapper -> wrapper.eq(Review::getStatus, "VISIBLE").or().isNull(Review::getStatus))
                .orderByDesc(Review::getCreatedAt));
        Set<Long> userIds = reviews.stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return reviews.stream().map(review -> toDetail(review, users.get(review.getUserId()))).toList();
    }

    public List<ReviewTask> myReviewTasks() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, user.getUserId())
                .eq(OrderInfo::getStatus, Constants.ORDER_COMPLETED)
                .orderByDesc(OrderInfo::getCompletedAt));
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(OrderInfo::getId).toList();
        Map<Long, OrderInfo> orderMap = orders.stream()
                .collect(Collectors.toMap(OrderInfo::getId, Function.identity()));
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByDesc(OrderItem::getId));
        Map<String, Review> reviewMap = new HashMap<>();
        reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, user.getUserId())
                        .in(Review::getOrderId, orderIds))
                .forEach(review -> reviewMap.put(reviewKey(review.getOrderId(), review.getProductId()), review));

        return items.stream().map(item -> {
            OrderInfo order = orderMap.get(item.getOrderId());
            Review review = reviewMap.get(reviewKey(item.getOrderId(), item.getProductId()));
            ReviewTask task = new ReviewTask();
            task.setOrderId(order.getId());
            task.setOrderNo(order.getOrderNo());
            task.setCompletedAt(order.getCompletedAt());
            task.setProductId(item.getProductId());
            task.setProductName(item.getProductName());
            task.setProductImage(item.getProductImage());
            task.setPrice(item.getPrice());
            task.setQuantity(item.getQuantity());
            task.setReviewed(review != null);
            if (review != null) {
                task.setReviewId(review.getId());
                task.setRating(review.getRating());
                task.setContent(review.getContent());
                task.setAppendContent(review.getAppendContent());
                task.setReviewedAt(review.getCreatedAt());
                task.setAppendAt(review.getAppendAt());
            }
            return task;
        }).toList();
    }

    public Review append(Long id, String content) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        Review review = reviewMapper.selectById(id);
        if (review == null || !review.getUserId().equals(user.getUserId())) {
            throw new BizException("评价不存在");
        }
        if (review.getAppendContent() != null && !review.getAppendContent().isBlank()) {
            throw new BizException("每条评价只能追评一次");
        }
        review.setAppendContent(content.trim());
        review.setAppendAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        return review;
    }

    public List<Review> merchantReviews() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        if (productMapper == null) {
            return List.of();
        }
        LambdaQueryWrapper<Product> productsQuery = new LambdaQueryWrapper<>();
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            productsQuery.eq(Product::getMerchantId, user.getMerchantId());
        }
        List<Long> productIds = productMapper.selectList(productsQuery).stream().map(Product::getId).toList();
        if (productIds.isEmpty()) {
            return List.of();
        }
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .in(Review::getProductId, productIds).orderByDesc(Review::getCreatedAt));
    }

    public Review reply(Long id, String content) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BizException("评价不存在");
        }
        Product product = productMapper.selectById(review.getProductId());
        if (product == null || Constants.ROLE_MERCHANT.equals(user.getRole())
                && !product.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能回复其他商家的评价");
        }
        review.setReply(content.trim());
        review.setReplyAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        logService.log("REVIEW", "REPLY", "商家回复评价：" + id);
        return review;
    }

    public List<Review> adminReviews() {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>().orderByDesc(Review::getCreatedAt));
    }

    public Review moderate(Long id, String status, String reason) {
        RoleGuard.requireRole(Constants.ROLE_SERVICE_ADMIN, Constants.ROLE_SUPER_ADMIN);
        if (!List.of("VISIBLE", "HIDDEN").contains(status)) {
            throw new BizException("评价状态不合法");
        }
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BizException("评价不存在");
        }
        review.setStatus(status);
        reviewMapper.updateById(review);
        logService.log("REVIEW", "MODERATE", "评价 " + id + " 改为 " + status + "：" + reason);
        return review;
    }

    private ProductReviewDetail toDetail(Review review, User user) {
        ProductReviewDetail detail = new ProductReviewDetail();
        detail.setId(review.getId());
        detail.setProductId(review.getProductId());
        detail.setRating(review.getRating());
        detail.setContent(review.getContent());
        detail.setAppendContent(review.getAppendContent());
        detail.setReply(review.getReply());
        detail.setStatus(review.getStatus());
        detail.setUserDisplayName(displayName(user));
        detail.setCreatedAt(review.getCreatedAt());
        detail.setAppendAt(review.getAppendAt());
        detail.setReplyAt(review.getReplyAt());
        return detail;
    }

    private String displayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        String name = user.getNickname();
        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }
        if (name == null || name.length() < 2) {
            return "匿名用户";
        }
        return name.substring(0, 1) + "***" + name.substring(name.length() - 1);
    }

    private String reviewKey(Long orderId, Long productId) {
        return orderId + ":" + productId;
    }
}
