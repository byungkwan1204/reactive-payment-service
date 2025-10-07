package com.example.paymentservice.payment.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;
    private BigDecimal amount;
    private int quantity;
    private String name;
    private Long sellerId;
}
