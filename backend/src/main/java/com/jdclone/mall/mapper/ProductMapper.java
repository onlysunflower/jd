package com.jdclone.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jdclone.mall.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {
    @Update("UPDATE product SET locked_stock = locked_stock + #{quantity} "
            + "WHERE id = #{productId} AND stock - locked_stock >= #{quantity}")
    int lockStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET locked_stock = locked_stock - #{quantity} "
            + "WHERE id = #{productId} AND locked_stock >= #{quantity}")
    int releaseLockedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock - #{quantity}, locked_stock = locked_stock - #{quantity}, sales = sales + #{quantity} "
            + "WHERE id = #{productId} AND stock >= #{quantity} AND locked_stock >= #{quantity}")
    int deductStockAndIncreaseSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
