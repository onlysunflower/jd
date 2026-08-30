package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jdclone.mall.common.BizException;
import com.jdclone.mall.common.Constants;
import com.jdclone.mall.dto.ProductRequest;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantProductServiceTest {
    @Mock private ProductMapper productMapper;
    @Mock private OperationLogService logService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void createSetsPendingAuditAndOffShelfForMerchant() {
        loginAsMerchant(1L, 50L);
        ProductRequest request = request("新款手机");

        ProductService service = new ProductService(productMapper, logService);
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(99L);
            return 1;
        });
        Product saved = service.create(request);

        assertEquals(Constants.PRODUCT_PENDING, saved.getAuditStatus());
        assertEquals(Constants.SHELF_OFF, saved.getShelfStatus());
        assertEquals(50L, saved.getMerchantId());
        assertEquals(0, saved.getSales());
        verify(productMapper).insert(any(Product.class));
    }

    @Test
    void updateRequiresOwnershipAndGoesBackToPending() {
        loginAsMerchant(1L, 50L);
        Product owned = product(9L, 50L, Constants.PRODUCT_APPROVED, Constants.SHELF_ON);
        when(productMapper.selectById(9L)).thenReturn(owned);

        ProductService service = new ProductService(productMapper, logService);
        Product updated = service.update(9L, request("改价商品"));

        assertEquals(Constants.PRODUCT_PENDING, updated.getAuditStatus());
        assertEquals(Constants.SHELF_OFF, updated.getShelfStatus());
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    void updateRejectsAnotherMerchantsProduct() {
        loginAsMerchant(1L, 50L);
        Product other = product(9L, 999L, Constants.PRODUCT_APPROVED, Constants.SHELF_ON);
        when(productMapper.selectById(9L)).thenReturn(other);

        ProductService service = new ProductService(productMapper, logService);
        assertThrows(BizException.class, () -> service.update(9L, request("越权商品")));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void onShelfOnlyAllowsApprovedProduct() {
        loginAsMerchant(1L, 50L);
        Product pending = product(10L, 50L, Constants.PRODUCT_PENDING, Constants.SHELF_OFF);
        when(productMapper.selectById(10L)).thenReturn(pending);

        ProductService service = new ProductService(productMapper, logService);
        assertThrows(BizException.class, () -> service.onShelf(10L));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void onShelfTurnsApprovedProductOffToOn() {
        loginAsMerchant(1L, 50L);
        Product approved = product(11L, 50L, Constants.PRODUCT_APPROVED, Constants.SHELF_OFF);
        when(productMapper.selectById(11L)).thenReturn(approved);

        ProductService service = new ProductService(productMapper, logService);
        Product result = service.onShelf(11L);

        assertEquals(Constants.SHELF_ON, result.getShelfStatus());
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    void onShelfRejectsUnapprovedForeignProductForMerchant() {
        loginAsMerchant(1L, 50L);
        Product approvedForeign = product(12L, 888L, Constants.PRODUCT_APPROVED, Constants.SHELF_OFF);
        when(productMapper.selectById(12L)).thenReturn(approvedForeign);

        ProductService service = new ProductService(productMapper, logService);
        assertThrows(BizException.class, () -> service.onShelf(12L));
    }

    @Test
    void offShelfRejectsAnotherMerchantsProduct() {
        loginAsMerchant(1L, 50L);
        Product foreign = product(13L, 777L, Constants.PRODUCT_APPROVED, Constants.SHELF_ON);
        when(productMapper.selectById(13L)).thenReturn(foreign);

        ProductService service = new ProductService(productMapper, logService);
        assertThrows(BizException.class, () -> service.offShelf(13L));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void merchantListingIsScopedToOwnMerchantId() {
        loginAsMerchant(1L, 50L);
        ProductService service = new ProductService(productMapper, logService);
        service.listForMerchant();
        verify(productMapper).selectList(any());
    }

    private void loginAsMerchant(Long userId, Long merchantId) {
        AuthContext.set(new AuthUser(userId, "shop-owner", Constants.ROLE_MERCHANT, merchantId));
    }

    private ProductRequest request(String name) {
        ProductRequest r = new ProductRequest();
        r.setCategoryId(1L);
        r.setName(name);
        r.setSubtitle("测试");
        r.setMainImage("https://example.com/a.png");
        r.setPrice(new BigDecimal("99.00"));
        r.setStock(10);
        return r;
    }

    private Product product(Long id, Long merchantId, String audit, String shelf) {
        Product p = new Product();
        p.setId(id);
        p.setMerchantId(merchantId);
        p.setAuditStatus(audit);
        p.setShelfStatus(shelf);
        p.setName("p" + id);
        return p;
    }
}
