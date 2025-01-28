package com.example.demo.entities;

import lombok.Data;
import lombok.Value;

@Value
@Data
public class Customer {
    private int customerId;
    private String callId;
    private long startTimestamp;
    private long endTimestamp;
}
