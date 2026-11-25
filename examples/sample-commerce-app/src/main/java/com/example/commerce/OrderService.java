package com.example.commerce;

public class OrderService {
    public String create(OrderRequest request) {
        if (request.total() < 10) throw new IllegalArgumentException("minimum order amount");
        return "created";
    }
}
