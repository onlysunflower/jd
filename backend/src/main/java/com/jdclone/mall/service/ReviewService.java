package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ReviewRequest;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.Review;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.ReviewMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderInfoMapper orderInfoMapper;

    public ReviewService(ReviewMapper reviewMapper, OrderInfoMapper orderInfoMapper) {
        this.reviewMapper = reviewMapper;
        this.orderInfoMapper = orderInfoMapper;
    }

    public Review create(ReviewRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        OrderInfo order = orderInfoMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(user.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new BizException("订单完成后才能评价");
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
        review.setContent(request.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        return review;
    }

    public List<Review> listByProduct(Long productId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .orderByDesc(Review::getCreatedAt));
    }
}
