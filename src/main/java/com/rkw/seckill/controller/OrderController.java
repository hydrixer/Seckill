package com.rkw.seckill.controller;

import com.rkw.seckill.entity.Product;
import com.rkw.seckill.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/product")
public class OrderController {

    private OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService=orderService;
    }

    @PostMapping("/add")
    public String addProduct(@RequestParam String name,
                             @RequestParam BigDecimal price,
                             @RequestParam Integer stock) {
        boolean success = orderService.addProduct(name,price,stock);
        return success? "操作完成":"操作失败";
    }

    @GetMapping("/showall")
    public List<Product> getProducts(){
        return orderService.getAllProducts();
    }

    @GetMapping("/stock/{id}")
    public ResponseEntity<Map<String, Object>> getProductStock(@PathVariable long id) {
        Map<String, Object> response = new HashMap<>();
        String stock = orderService.getProductStockFromRedis(id);

        if (stock == null) {
            response.put("success", false);
            response.put("message", "库存信息未缓存");
            response.put("stock", 0);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        response.put("success", true);
        response.put("stock", stock);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getproduct")
    public Product getProductById(@RequestParam long id){
        return orderService.getProductById(id);
    }

    @Transactional
    @RequestMapping("/update/{id}")
    public String changeStock(@PathVariable long id, @RequestParam Integer newStock){
        boolean success = orderService.updateStock(id,newStock);
        return success? "操作完成":"操作失败";
    }

    @Transactional
    @RequestMapping("buy/{id}")
    public String reduceStock(@PathVariable long id){
        return orderService.reduceStock(id);
    }
}
