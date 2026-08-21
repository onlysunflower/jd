package com.jdclone.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("refund_log")
public class RefundLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long refundId;
    private String operatorRole;
    private Long operatorId;
    private String action;
    private String remark;
    private LocalDateTime createdAt;
}
