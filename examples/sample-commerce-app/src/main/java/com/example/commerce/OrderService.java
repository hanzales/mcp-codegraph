package com.example.commerce;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final PaymentService payments;
    private final CommerceProperties properties;

    public OrderService(OrderRepository repository, PaymentService payments, CommerceProperties properties) {
        this.repository = repository; this.payments = payments; this.properties = properties;
    }

    @Transactional
    public String create(OrderRequest request) {
        if (request.total() < properties.minimumOrderCents()) throw new IllegalArgumentException("minimum order amount");
        var order = new Order(); payments.authorize(order); repository.save(order);
        return "created";
    }
}
