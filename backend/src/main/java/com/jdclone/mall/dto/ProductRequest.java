package com.jdclone.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductRequest {
    private Long categoryId;
    @NotBlank
    private String name;
    private String subtitle;
    private String mainImage;
    @NotNull
    private BigDecimal price;
    @NotNull
    @Min(0)
    private Integer stock;
    private List<@Valid ProductSkuRequest> skus;
}
