package com.jdclone.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jdclone.mall.entity.Merchant;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MerchantMapper extends BaseMapper<Merchant> {
    @Update("UPDATE merchant SET frozen_balance = frozen_balance + #{amount} WHERE id = #{merchantId}")
    int addFrozen(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    @Update("UPDATE merchant SET available_balance = available_balance + #{amount}, "
            + "frozen_balance = GREATEST(frozen_balance - #{amount}, 0) WHERE id = #{merchantId}")
    int releaseBalance(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    @Update("UPDATE merchant SET available_balance = available_balance - #{amount} "
            + "WHERE id = #{merchantId} AND available_balance >= #{amount}")
    int deductAvailable(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    @Update("UPDATE merchant SET available_balance = available_balance + #{amount} WHERE id = #{merchantId}")
    int restoreAvailable(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    @Update("UPDATE merchant SET frozen_balance = GREATEST(frozen_balance - #{amount}, 0) WHERE id = #{merchantId}")
    int reverseFrozen(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    @Update("UPDATE merchant SET available_balance = GREATEST(available_balance - #{amount}, 0) WHERE id = #{merchantId}")
    int reverseAvailable(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);
}
