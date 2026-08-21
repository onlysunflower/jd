package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.CartAddRequest;
import com.jdclone.mall.entity.CartItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.CartItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    public List<CartItem> list() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getUserId())
                .orderByDesc(CartItem::getUpdatedAt));
    }

    public CartItem add(CartAddRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || !Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())
                || !Constants.SHELF_ON.equals(product.getShelfStatus())) {
            throw new BizException("商品不可购买");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new BizException("商品库存不足");
        }
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getUserId())
                .eq(CartItem::getProductId, request.getProductId()));
        if (item == null) {
            item = new CartItem();
            item.setUserId(user.getUserId());
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            item.setCreatedAt(LocalDateTime.now());
        } else {
            item.setQuantity(item.getQuantity() + request.getQuantity());
        }
        item.setUpdatedAt(LocalDateTime.now());
        if (item.getId() == null) {
            cartItemMapper.insert(item);
        } else {
            cartItemMapper.updateById(item);
        }
        return item;
    }

    public CartItem updateQuantity(Long id, Integer quantity) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(user.getUserId())) {
            throw new BizException("购物车商品不存在");
        }
        item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        return item;
    }

    public void remove(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        CartItem item = cartItemMapper.selectById(id);
        if (item != null && item.getUserId().equals(user.getUserId())) {
            cartItemMapper.deleteById(id);
        }
    }
}
