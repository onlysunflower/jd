package com.jdclone.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jdclone.mall.entity.OrderInfo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    @Update("UPDATE order_info SET status = 'WAIT_SHIP', paid_at = #{paidAt}, "
            + "payment_no = CONCAT('PAY', id, DATE_FORMAT(#{paidAt}, '%Y%m%d%H%i%s')) "
            + "WHERE id = #{id} AND status = 'WAIT_PAY'")
    int markPaidIfWaiting(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt);
}
