package com.jdclone.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnLogisticsRequest {
    @NotBlank
    private String returnLogisticsNo;
}
