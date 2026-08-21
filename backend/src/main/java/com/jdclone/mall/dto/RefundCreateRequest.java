package com.jdclone.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundCreateRequest {
    @NotNull
    private Long orderId;
    @NotBlank
    private String type;
    @NotBlank
    private String reason;
    private String evidenceImages;
}
