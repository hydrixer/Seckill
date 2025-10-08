package com.rkw.seckill.service;

import com.rkw.seckill.mapper.OrderMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaConsumer {

    private final OrderMapper orderMapper;

    private final RedisService redisService;
    private final OrderService orderService;

    public KafkaConsumer(OrderMapper orderMapper, OrderService orderService, RedisService redisService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.redisService = redisService;
    }

    @KafkaListener(groupId = "seckill-group", topics = "seckill")
    public void consumeMessage(String message) {
        System.out.println("Received message: " + message);
        String[] parts = message.split(":");
        Long productId = Long.parseLong(parts[0]);
        String uid = parts[1];  // 可选定义购买数量

        if(redisService.hasKey("msg"+uid)){
            return;
        }

        // 扣减库存（数据库）
        orderService.updateDatabaseStock(productId);
        redisService.set("msg"+uid,"done");
    }




}
