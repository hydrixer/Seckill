package com.rkw.seckill.service;

import com.rkw.seckill.entity.Product;
import com.rkw.seckill.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final RedisService redisService;
    private final KafkaProducer kafkaProducer;

    public OrderService(OrderMapper orderMapper, RedisService redisService, KafkaProducer kafkaProducer) {
        this.orderMapper = orderMapper;
        this.redisService = redisService;
        this.kafkaProducer = kafkaProducer;
    }

    public Product getProductById(Long id) {
        String key = "product:" + id;
        Product product = (Product) redisService.get(key);
        if (product != null) return product;

        product = orderMapper.findById(id);
        if (product != null) {
            redisService.set(key, product, 300, TimeUnit.SECONDS);
            redisService.set2("product_stock:" + id, product.getStock(), 3600, TimeUnit.SECONDS);
        }
        return product;
    }

    // 查询所有商品
    public List<Product> getAllProducts() {
        String productListKey = "product:list";
        List<Product> products = (List<Product>) redisService.get(productListKey);

        if (products == null) {
            return new ArrayList<>(); // Redis中没有数据时返回空列表
        }

        // 为每个商品更新实时库存（从Redis中获取）
//        for (Product product : products) {
//            String stockKey = "product_stock:" + product.getId();
//            Object stockObj = redisService.get(stockKey);
//            if (stockObj != null) {
//                product.setStock(((Number) stockObj).intValue());
//            }
//        }

        return products;
    }

    // 只从Redis获取库存
    public String getProductStockFromRedis(Long productId) {
        String stockKey = "product_stock:" + productId;
        Object stockObj = redisService.get(stockKey);
        if (stockObj != null) {
//            return ((Number) stockObj).intValue();
        return  String.valueOf(stockObj);
        }
        return null; // Redis中没有库存信息
    }

    @Transactional
    public boolean addProduct(String name, BigDecimal price, Integer stock) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        int result = orderMapper.addProduct(product);

        return result > 0;
    }


    /**
     * 预热商品数据到Redis（供后台调用）
     */
    public int preloadProductDataToRedis() {
        List<Product> products = orderMapper.findProducts();

        if (products != null && !products.isEmpty()) {
            // 缓存商品列表
            String productListKey = "product:list";
            redisService.set(productListKey, products, -1, TimeUnit.SECONDS);

            // 缓存每个商品的库存
            for (Product product : products) {
                String stockKey = "product_stock:" + product.getId();
                redisService.set2(stockKey, String.valueOf(product.getStock()), -1, TimeUnit.SECONDS);
            }

            return products.size();
        }

        return 0;
    }

    // 更新库存（手动设置具体值）
    // 设定为需要在预热前更新
    // 删除注释以实现实时更新
    @Transactional
    public boolean updateStock(Long productId, Integer newStock) {
        int affected = orderMapper.updateStock(productId, newStock);
//        if (affected > 0) {
//            // 更新成功后同步Redis
//            String key = "product_stock:" + productId;
//            redisService.set(key, newStock, 3600, TimeUnit.SECONDS);
//
//            // 同时更新产品缓存
//            Product product = orderMapper.findById(productId);
//            if (product != null) {
//                product.setStock(newStock);
//                redisService.set("product:" + productId, product, 300, TimeUnit.SECONDS);
//            }
//        }
        return affected > 0;
    }

    // 秒杀扣库存 - Redis优先策略
    public String reduceStock(Long productId) {
//        return redisService.test1();

        String stockKey = "product_stock:" + productId;

//        long result = redisService.decrementByLua(stockKey,1);
        long result = redisService.decrementByLua(stockKey,1);  //amount暂时没用，默认限购1
        // 扣减库存（Redis）
        if(result==-2){
            return "库存不足";
        } else if (result==-1) {
            return "商品不存在";
        }

        try {
            // 同步更新数据库
            kafkaProducer.sendMessage(productId.toString());
//            updateDatabaseStock(productId);
        } catch (Exception e) {
            // 数据库更新失败 → 回滚 Redis
            redisService.increment(stockKey);
            return "操作失败，请重试";
        }
        return "操作成功";
    }

    // 异步更新数据库库存

    @Transactional
    public void updateDatabaseStock(Long productId) {
        orderMapper.reduceStock(productId);
    }

    /**
     * 清除所有商品相关的Redis缓存
     * 完全删除所有内容，包括商品列表和库存信息
     */
    public int clearAllProductCache() {
        List<Product> products = getAllProducts();
        int count = 0;
        for (Product product : products) {
            // 删除商品信息缓存
//            String productKey = "product:" + product.getId();
//            if (redisService.hasKey(productKey)) {
//                redisService.delete(productKey);
//                count++;
//            }
            String productKey = "product:list";
            if (redisService.hasKey(productKey)) {
                redisService.delete(productKey);
            }

            // 删除商品库存缓存
            String stockKey = "product_stock:" + product.getId();
            if (redisService.hasKey(stockKey)) {
                redisService.delete(stockKey);
                count++;
            }
        }
        return count;
    }
}
