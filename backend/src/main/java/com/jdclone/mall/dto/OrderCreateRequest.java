package com.jdclone.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotNull(message = "商品不能为空")
    private Long productId;
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须为正整数")
    private Integer quantity;
    private Long addressId;
    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人不能超过64个字符")
    private String receiver;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的中国大陆手机号")
    private String receiverPhone;
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址不能超过255个字符")
    private String receiverAddress;
}
