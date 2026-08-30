package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.CartAddRequest;
import com.jdclone.mall.dto.CartItemDetail;
import com.jdclone.mall.entity.CartItem;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.entity.ProductSku;
import com.jdclone.mall.mapper.CartItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.mapper.ProductSkuMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this(cartItemMapper, productMapper, null);
    }

    @Autowired
    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper, ProductSkuMapper productSkuMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
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
        ProductSku sku = findSku(product, request.getSkuId());
        int available = availableStock(product, sku);
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getUserId())
                .eq(CartItem::getProductId, request.getProductId())
                .eq(sku != null, CartItem::getSkuId, sku == null ? null : sku.getId())
                .isNull(sku == null, CartItem::getSkuId));
        int currentQuantity = item == null ? 0 : item.getQuantity();
        if (currentQuantity > available - request.getQuantity()) {
            throw new BizException("购物车商品数量不能超过当前库存");
        }
        if (item == null) {
            item = new CartItem();
            item.setUserId(user.getUserId());
            item.setProductId(request.getProductId());
            item.setSkuId(sku == null ? null : sku.getId());
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
        ProductSku sku = findSku(product, item.getSkuId());
        if (quantity > availableStock(product, sku)) {
            throw new BizException("购物车商品数量不能超过当前库存");
        }
        item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        return toDetail(item, product, sku);
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
        Product product = productMapper.selectById(item.getProductId());
        ProductSku sku = product == null ? null : findSku(product, item.getSkuId());
        return toDetail(item, product, sku);
    }

    private CartItemDetail toDetail(CartItem item, Product product, ProductSku sku) {
        CartItemDetail detail = new CartItemDetail();
        detail.setId(item.getId());
        detail.setProductId(item.getProductId());
        detail.setSkuId(item.getSkuId());
        detail.setQuantity(item.getQuantity());
        if (product == null) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已删除");
            return detail;
        }
        detail.setProductName(product.getName());
        detail.setProductImage(product.getMainImage());
        detail.setSkuCode(sku == null ? null : sku.getSkuCode());
        detail.setSpecName(sku == null ? "默认规格" : sku.getSpecName());
        detail.setPrice(sku == null ? product.getPrice() : sku.getPrice());
        int available = availableStock(product, sku);
        detail.setStock(sku == null ? product.getStock() : sku.getStock());
        detail.setAvailableStock(available);
        if (!Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品未审核通过");
        } else if (!Constants.SHELF_ON.equals(product.getShelfStatus())) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已下架");
        } else if (available <= 0) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("商品已售罄");
        } else if (item.getQuantity() > available) {
            detail.setPurchasable(false);
            detail.setUnavailableReason("购物车数量超过当前库存");
        } else {
            detail.setPurchasable(true);
        }
        return detail;
    }

    private ProductSku findSku(Product product, Long skuId) {
        if (productSkuMapper == null) {
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
            throw new BizException("所选商品规格不可购买");
        }
        return sku;
    }

    private int availableStock(Product product, ProductSku sku) {
        if (sku == null) {
            return product.getStock() == null ? 0 : product.getStock();
        }
        return Math.max(0, sku.getStock() - (sku.getLockedStock() == null ? 0 : sku.getLockedStock()));
    }
}
