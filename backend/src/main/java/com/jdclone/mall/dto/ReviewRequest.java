package com.jdclone.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Long productId;
    @Min(1)
    @Max(5)
    private Integer rating;
    private String content;
}
