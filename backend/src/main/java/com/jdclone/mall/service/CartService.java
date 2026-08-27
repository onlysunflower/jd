package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.CartAddRequest;
import com.jdclone.mall.dto.CartItemDetail;
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

    public List<CartItemDetail> list() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getUserId())
                .orderByDesc(CartItem::getUpdatedAt))
                .stream()
                .map(this::toDetail)
                .toList();
    }

    public CartItem add(CartAddRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        Product product = productMapper.selectById(request.getProductId());
        ensurePositiveQuantity(request.getQuantity());
        ensurePurchasable(product);
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getUserId())
                .eq(CartItem::getProductId, request.getProductId()));
        int currentQuantity = item == null ? 0 : item.getQuantity();
        if (currentQuantity > product.getStock() - request.getQuantity()) {
            throw new BizException("购物车商品数量不能超过当前库存");
        }
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

    public CartItemDetail updateQuantity(Long id, Integer quantity) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new BizException(404, "购物车商品不存在");
        }
        if (!item.getUserId().equals(user.getUserId())) {
            throw new BizException(403, "不能修改其他用户的购物车商品");
        }
        ensurePositiveQuantity(quantity);
        Product product = productMapper.selectById(item.getProductId());
        ensurePurchasable(product);
        if (quantity > product.getStock()) {
            throw new BizException("购物车商品数量不能超过当前库存");
        }
        item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        return toDetail(item, product);
    }

    public void remove(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_USER);
        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new BizException(404, "购物车商品不存在");
        }
        if (!item.getUserId().equals(user.getUserId())) {
            throw new BizException(403, "不能删除其他用户的购物车商品");
        }
        cartItemMapper.deleteById(id);
    }

    private void ensurePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("购买数量必须为正整数");
        }
    }

    private void ensurePurchasable(Product product) {
        if (product == null) {
            throw new BizException("商品不存在或已删除");
        }
        if (!Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())) {
            throw new BizException("商品未审核通过，暂不可购买");
        }
        if (!Constants.SHELF_ON.equals(product.getShelfStatus())) {
            throw new BizException("商品已下架，暂不可购买");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BizException("商品库存不足");
        }
    }

    private CartItemDetail toDetail(CartItem item) {
        return toDetail(item, productMapper.selectById(item.getProductId()));
    }

    private CartItemDetail toDetail(CartItem item, Product product) {
        CartItemDetail detail = new CartItemDetail();
        detail.setId(item.getId());
        detail.setProductId(item.getProductId());
        detail.setQuantity(item.getQuantity());
        if (product == null) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已删除");
            return detail;
        }
        detail.setProductName(product.getName());
        detail.setProductImage(product.getMainImage());
        detail.setPrice(product.getPrice());
        detail.setStock(product.getStock());
        if (!Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品未审核通过");
        } else if (!Constants.SHELF_ON.equals(product.getShelfStatus())) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已下架");
        } else if (product.getStock() == null || product.getStock() <= 0) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已售罄");
        } else if (item.getQuantity() > product.getStock()) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("购物车数量超过当前库存");
        } else {
            detail.setPurchasable(true);
        }
        return detail;
    }
}
