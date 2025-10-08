package com.rkw.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Product {
    private Long id;
    private BigDecimal price;
    private String name;
    private Integer stock;
}
