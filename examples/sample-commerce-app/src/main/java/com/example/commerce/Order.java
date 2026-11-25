package com.example.commerce;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
@Entity @Table(name = "orders")
public class Order { @Id private Long id; private int totalCents; @ManyToOne private Customer customer; public int getTotalCents() { return totalCents; } }
