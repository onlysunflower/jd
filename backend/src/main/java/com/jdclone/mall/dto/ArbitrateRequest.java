package com.jdclone.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArbitrateRequest {
    @NotBlank
    private String decision;
    private String remark;
}
