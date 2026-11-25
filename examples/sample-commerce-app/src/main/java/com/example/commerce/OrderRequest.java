package com.example.commerce;

import jakarta.validation.constraints.Min;

public record OrderRequest(@Min(10) int total) { }
