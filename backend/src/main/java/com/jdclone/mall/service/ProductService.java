package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ProductRequest;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final OperationLogService logService;

    public ProductService(ProductMapper productMapper, OperationLogService logService) {
        this.productMapper = productMapper;
        this.logService = logService;
    }

    public List<Product> listForUser(String keyword, Long categoryId) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getAuditStatus, Constants.PRODUCT_APPROVED)
                .eq(Product::getShelfStatus, Constants.SHELF_ON)
                .orderByDesc(Product::getUpdatedAt);
        if (keyword != null && !keyword.isBlank()) {
            query.like(Product::getName, keyword);
        }
        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }
        return productMapper.selectList(query);
    }

    public Product publicDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())
                || !Constants.SHELF_ON.equals(product.getShelfStatus())) {
            throw new BizException("商品不存在或不可购买");
        }
        return product;
    }

    private Product findByIdOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return product;
    }

    public List<Product> listForMerchant() {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().orderByDesc(Product::getUpdatedAt);
        if (Constants.ROLE_MERCHANT.equals(user.getRole())) {
            query.eq(Product::getMerchantId, user.getMerchantId());
        }
        return productMapper.selectList(query);
    }

    public Product create(ProductRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        Product product = new Product();
        product.setMerchantId(user.getMerchantId());
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImage(request.getMainImage());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSales(0);
        product.setAuditStatus(Constants.PRODUCT_PENDING);
        product.setShelfStatus(Constants.SHELF_OFF);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        logService.log("PRODUCT", "CREATE", "商家提交商品：" + product.getName());
        return product;
    }

    public Product update(Long id, ProductRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        if (Constants.ROLE_MERCHANT.equals(user.getRole()) && !product.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能编辑其他商家的商品");
        }
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImage(request.getMainImage());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setAuditStatus(Constants.PRODUCT_PENDING);
        product.setShelfStatus(Constants.SHELF_OFF);
        product.setRejectReason(null);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        logService.log("PRODUCT", "UPDATE", "商品修改后重新进入审核：" + product.getName());
        return product;
    }

    public List<Product> pending() {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getAuditStatus, Constants.PRODUCT_PENDING)
                .orderByDesc(Product::getUpdatedAt));
    }

    public Product approve(Long id) {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        product.setAuditStatus(Constants.PRODUCT_APPROVED);
        product.setShelfStatus(Constants.SHELF_ON);
        product.setRejectReason(null);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        logService.log("PRODUCT", "APPROVE", "审核通过商品：" + product.getName());
        return product;
    }

    public Product reject(Long id, String reason) {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        product.setAuditStatus(Constants.PRODUCT_REJECTED);
        product.setShelfStatus(Constants.SHELF_OFF);
        product.setRejectReason(reason);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        logService.log("PRODUCT", "REJECT", "驳回商品：" + product.getName() + "，原因：" + reason);
        return product;
    }

    public Product offShelf(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        if (Constants.ROLE_MERCHANT.equals(user.getRole()) && !product.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能下架其他商家的商品");
        }
        product.setShelfStatus(Constants.SHELF_OFF);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        logService.log("PRODUCT", "OFF_SHELF", "下架商品：" + product.getName());
        return product;
    }

    public Product onShelf(Long id) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        if (Constants.ROLE_MERCHANT.equals(user.getRole()) && !product.getMerchantId().equals(user.getMerchantId())) {
            throw new BizException(403, "不能上架其他商家的商品");
        }
        if (!Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())) {
            throw new BizException("只有审核通过的商品才能上架");
        }
        product.setShelfStatus(Constants.SHELF_ON);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        logService.log("PRODUCT", "ON_SHELF", "上架商品：" + product.getName());
        return product;
    }
}
