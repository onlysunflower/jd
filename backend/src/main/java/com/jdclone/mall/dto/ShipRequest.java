package com.jdclone.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShipRequest {
    @NotBlank
    private String logisticsCompany;
    @NotBlank
    private String logisticsNo;
}
