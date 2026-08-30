package com.jdclone.mall.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 用户购物车项及其当前商品展示信息。
 * 商品信息在查询时实时读取，不作为购物车快照保存。
 */
@Data
public class CartItemDetail {
    private Long id;
    private Long productId;
    private Long skuId;
    private String skuCode;
    private String specName;
    private Integer quantity;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer stock;
    private Integer availableStock;
    private boolean purchasable;
    private String unavailableReason;
}
