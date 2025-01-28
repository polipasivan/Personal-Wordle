package com.example.demo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerInfo {
    private int customerId;
    private String date;
    private int maxConcurrentCalls;
    private long timestamp;
    private List<String> callIds;
}
