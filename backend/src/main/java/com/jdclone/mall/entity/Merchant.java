package com.jdclone.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String licenseNo;
    private String licenseImage;
    private BigDecimal commissionRate;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
}
