package com.rkw.seckill.controller;

import com.rkw.seckill.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {


    private final OrderService orderService;

    public AdminController(OrderService orderService){
        this.orderService=orderService;
    }


    /**
     * 一键预热所有商品到Redis
     */
    @PostMapping("/preload")
    public ResponseEntity<Map<String, Object>> preloadAllProducts() {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = orderService.preloadProductDataToRedis();
            result.put("success", true);
            result.put("message", "预热成功，共预热" + count + "个商品");
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "预热失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 一键清除所有商品的Redis缓存
     */
    @DeleteMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = orderService.clearAllProductCache();
            result.put("success", true);
            result.put("message", "清除缓存成功，共清除" + count + "个商品缓存");
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除缓存失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}

