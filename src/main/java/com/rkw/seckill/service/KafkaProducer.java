package com.rkw.seckill.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String id) {
        String uid = UUID.randomUUID().toString();
        String message = id + ":" + uid;
        kafkaTemplate.send("seckill", message).whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Message sent successfully: " + message);
            } else {
                System.err.println("Failed to send message: " + message);
                ex.printStackTrace();
            }
        });
    }
}
