package com.example.commerce;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity @Table(name = "customers")
public class Customer { @Id private Long id; private String status; public Long getId() { return id; } }
