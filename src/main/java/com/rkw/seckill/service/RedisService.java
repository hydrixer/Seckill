package com.rkw.seckill.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 设置键值对
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置带过期时间的键值对
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置带过期时间的键值对
     * 字符串专用，用于stock，lua
     */
    public void set2(String key, Object value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, (String) value, timeout, unit);
    }

    /**
     * 获取指定键的值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 判断键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 原子性递减操作（用于库存扣减）
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 使用 Lua 脚本原子扣减库存
     * @param key 库存 key
     * @param amount 扣减数量
     * @return 剩余库存：
     *         >0 扣减成功后的库存
     *         0 库存不足
     *         -1 Redis中不存在库存
     */
    public Long decrementByLua(String key, long amount) {
        //自定义amount数量
//        String luaScript = ""
//                + "local stockStr = redis.call('GET', KEYS[1])\n"
//                + "if not stockStr then\n"
//                + "  return -1\n"
//                + "end\n"
//                + "local stock = tonumber(stockStr)\n"
//                + "local arg1 = tonumber(ARGV[1])\n"
//                + "if not stock then\n"
//                + "  return -1\n"
//                + "end\n"
//                + "if stock >= arg1 then\n"
//                + "  redis.call('DECRBY', KEYS[1], ARGV[1])\n"
//                + "  return stock - arg1\n"
//                + "else\n"
//                + "  return -2\n"
//                + "end";
        String luaScript = """
                local stock = tonumber(redis.call('GET', KEYS[1]))
                if not stock then
                  return -1
                end
                if stock <= 0 then
                  return -2
                end
                local newStock = redis.call('DECR', KEYS[1])
                return newStock""";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        return redisTemplate.execute(redisScript,
                Collections.singletonList(key),
                amount);
    }

    public String test1(){
        String val = redisTemplate.opsForValue().get("product_stock:1").toString();
        System.out.println("Redis value: " + val);
        return  val;
    }
    /**
     * 原子性递增操作
     */
    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
}
