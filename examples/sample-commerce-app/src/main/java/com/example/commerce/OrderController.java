package com.example.commerce;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }
    @PostMapping("/orders")
    public String create(@Valid OrderRequest request) { return orders.create(request); }
}
