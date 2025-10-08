package com.rkw.seckill.mapper;

import com.rkw.seckill.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int addProduct(Product product);
    List<Product> findProducts();
    Product findById(@Param("id") long id);

    int updateStock(@Param("id") long id, @Param("stock") Integer stock);

    int reduceStock(@Param("id") long id);
}
