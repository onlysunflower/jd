package com.jdclone.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ProductRequest;
import com.jdclone.mall.dto.ProductSkuRequest;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.entity.ProductSku;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.mapper.ProductSkuMapper;
import com.jdclone.mall.security.AuthUser;
import com.jdclone.mall.security.RoleGuard;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OperationLogService logService;

    public ProductService(ProductMapper productMapper, ProductSkuMapper productSkuMapper, OperationLogService logService) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
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
        return attachSkus(productMapper.selectList(query), true);
    }

    public Product publicDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Constants.PRODUCT_APPROVED.equals(product.getAuditStatus())
                || !Constants.SHELF_ON.equals(product.getShelfStatus())) {
            throw new BizException("商品不存在或不可购买");
        }
        attachSkus(product, true);
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
        return attachSkus(productMapper.selectList(query), false);
    }

    public Product create(ProductRequest request) {
        AuthUser user = RoleGuard.requireRole(Constants.ROLE_MERCHANT, Constants.ROLE_SUPER_ADMIN);
        Product product = new Product();
        product.setMerchantId(user.getMerchantId());
        product.setCategoryId(request.getCategoryId());
        product.setSpuCode("SPU" + System.currentTimeMillis());
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
        syncSkus(product, request.getSkus());
        logService.log("PRODUCT", "CREATE", "商家提交商品：" + product.getName());
        attachSkus(product);
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
        syncSkus(product, request.getSkus());
        logService.log("PRODUCT", "UPDATE", "商品修改后重新进入审核：" + product.getName());
        attachSkus(product);
        return product;
    }

    public List<Product> pending() {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return attachSkus(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getAuditStatus, Constants.PRODUCT_PENDING)
                .orderByDesc(Product::getUpdatedAt)), false);
    }

    public Product approve(Long id) {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        product.setAuditStatus(Constants.PRODUCT_APPROVED);
        product.setShelfStatus(Constants.SHELF_ON);
        product.setRejectReason(null);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        updateSkuStatus(product.getId(), Constants.SHELF_ON);
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
        updateSkuStatus(product.getId(), Constants.SHELF_OFF);
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
        updateSkuStatus(product.getId(), Constants.SHELF_OFF);
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
        updateSkuStatus(product.getId(), Constants.SHELF_ON);
        logService.log("PRODUCT", "ON_SHELF", "上架商品：" + product.getName());
        return product;
    }

    public Product forceOffShelf(Long id, String reason) {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        Product product = findByIdOrThrow(id);
        product.setShelfStatus(Constants.SHELF_OFF);
        product.setRejectReason(reason);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        updateSkuStatus(product.getId(), Constants.SHELF_OFF);
        logService.log("PRODUCT", "FORCE_OFF_SHELF", "平台强制下架商品：" + product.getName() + "，原因：" + reason);
        return product;
    }

    public List<Product> listForAdmin() {
        RoleGuard.requireRole(Constants.ROLE_PRODUCT_ADMIN, Constants.ROLE_SUPER_ADMIN);
        return attachSkus(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getUpdatedAt)), false);
    }

    private List<Product> attachSkus(List<Product> products, boolean onlyOnShelf) {
        products.forEach(product -> {
            List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, product.getId())
                    .eq(onlyOnShelf, ProductSku::getStatus, Constants.SHELF_ON)
                    .orderByAsc(ProductSku::getId));
            product.setSkus(skus);
        });
        return products;
    }

    private void attachSkus(Product product) {
        attachSkus(product, false);
    }

    private void attachSkus(Product product, boolean onlyOnShelf) {
        product.setSkus(productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, product.getId())
                .eq(onlyOnShelf, ProductSku::getStatus, Constants.SHELF_ON)
                .orderByAsc(ProductSku::getId)));
    }

    private void syncSkus(Product product, List<ProductSkuRequest> requests) {
        List<ProductSkuRequest> values = requests == null || requests.isEmpty()
                ? List.of(defaultSku(product)) : requests;
        Map<Long, ProductSku> existing = new HashMap<>();
        productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId()))
                .forEach(sku -> existing.put(sku.getId(), sku));
        BigDecimalSummary summary = new BigDecimalSummary();
        for (int i = 0; i < values.size(); i++) {
            ProductSkuRequest request = values.get(i);
            ProductSku sku = request.getId() == null ? null : existing.remove(request.getId());
            if (sku == null) {
                sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSkuCode(request.getSkuCode() == null || request.getSkuCode().isBlank()
                        ? product.getSpuCode() + "-" + (i + 1) : request.getSkuCode());
                sku.setLockedStock(0);
                sku.setSales(0);
                sku.setCreatedAt(LocalDateTime.now());
            }
            if (request.getStock() < (sku.getLockedStock() == null ? 0 : sku.getLockedStock())) {
                throw new BizException("SKU 库存不能小于已锁定库存");
            }
            sku.setSpecName(request.getSpecName());
            sku.setPrice(request.getPrice());
            sku.setStock(request.getStock());
            sku.setStatus(Constants.SHELF_OFF);
            sku.setUpdatedAt(LocalDateTime.now());
            if (sku.getId() == null) {
                productSkuMapper.insert(sku);
            } else {
                productSkuMapper.updateById(sku);
            }
            summary.add(request.getPrice(), request.getStock());
        }
        existing.values().forEach(sku -> {
            sku.setStatus(Constants.SHELF_OFF);
            productSkuMapper.updateById(sku);
        });
        product.setPrice(summary.minPrice);
        product.setStock(summary.totalStock);
        productMapper.updateById(product);
    }

    private ProductSkuRequest defaultSku(Product product) {
        ProductSkuRequest request = new ProductSkuRequest();
        request.setSpecName("默认规格");
        request.setPrice(product.getPrice());
        request.setStock(product.getStock());
        return request;
    }

    private void updateSkuStatus(Long productId, String status) {
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId));
        skus.forEach(sku -> {
            sku.setStatus(status);
            sku.setUpdatedAt(LocalDateTime.now());
            productSkuMapper.updateById(sku);
        });
    }

    private static class BigDecimalSummary {
        private java.math.BigDecimal minPrice;
        private int totalStock;

        private void add(java.math.BigDecimal price, int stock) {
            minPrice = minPrice == null || price.compareTo(minPrice) < 0 ? price : minPrice;
            totalStock += stock;
        }
    }
}
