package com.example.demo.controller;

import com.example.demo.entities.Customer;
import com.example.demo.entities.Customers;
import com.example.demo.response.CustomerInfo;
import com.example.demo.response.Results;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@Slf4j
public class Controller {

    @GetMapping(value = "/getSomething")
    public Results getRecords() {
        RestTemplate restTemplate = new RestTemplate();
        var response = restTemplate.getForEntity("https://candidate.hubteam.com/candidateTest/v3/problem/dataset?userKey=5831429e466fe642ea1bacd0b5bf", Customers.class).getBody();

        var test = restTemplate.getForEntity("https://candidate.hubteam.com/candidateTest/v3/problem/test-dataset?userKey=5831429e466fe642ea1bacd0b5bf", String.class).getBody();


        MultiKeyMap<Object, List<Customer>> multiKeyMap = new MultiKeyMap<>();
        response.getCallRecords().stream().forEach( record -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String startDate = dateFormat.format(record.getStartTimestamp());
            String endDate = dateFormat.format(record.getEndTimestamp());

            if(startDate.equals(endDate)) {
                if(!multiKeyMap.containsKey(record.getCustomerId(), startDate)) {
                    List<Customer> customers = new ArrayList<>();
                    customers.add(record);
                    multiKeyMap.put(record.getCustomerId(), startDate, customers);
                }
                else {
                    List<Customer> customerRecord = multiKeyMap.get(record.getCustomerId(), startDate);
                    customerRecord.add(record);
                    multiKeyMap.put(record.getCustomerId(), startDate, customerRecord);
                }
            }
            else {
                if(!multiKeyMap.containsKey(record.getCustomerId(), startDate)) {
                    List<Customer> customers = new ArrayList<>();
                    customers.add(record);
                    multiKeyMap.put(record.getCustomerId(), startDate, customers);
                }
                else if (!multiKeyMap.containsKey(record.getCustomerId(), endDate)) {
                    List<Customer> customers = new ArrayList<>();
                    customers.add(record);
                    multiKeyMap.put(record.getCustomerId(), endDate, customers);
                }
                else {
                    List<Customer> customerRecordStart = multiKeyMap.get(record.getCustomerId(), startDate);
                    customerRecordStart.add(record);
                    List<Customer> customerRecordEnd = multiKeyMap.get(record.getCustomerId(), startDate);
                    customerRecordEnd.add(record);
                    multiKeyMap.put(record.getCustomerId(), startDate, customerRecordStart);
                    multiKeyMap.put(record.getCustomerId(), startDate, customerRecordEnd);
                }
            }
        });

        Results results = new Results();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for (Map.Entry<MultiKey<?>, List<Customer>> entry : multiKeyMap.entrySet()) {
            String dayDate = entry.getKey().getKey(1).toString();
            String customerId = entry.getKey().getKey(0).toString();
            List<Customer> records = entry.getValue();
            HashMap<String, List<Customer>> map = new HashMap<>();
            for (int i = 0; i < records.size(); i++) {
                Customer record1 = records.get(i);
                map.put(record1.getCallId(), new ArrayList<>());
                for (int j = i + 1; j < records.size(); j++) {
                    Customer record2 = records.get(j);

                    // Check for overlap
                    if (datesOverlap(record1.getStartTimestamp(), record1.getEndTimestamp(),
                            record2.getStartTimestamp(), record2.getEndTimestamp())) {
                        map.get(record1.getCallId()).add(record2);
                    }
                }
            }

            int highestValue = 0;
            List<Customer> highestValueList = new ArrayList<>();
            for (Map.Entry<String, List<Customer>> entry1 : map.entrySet()) {
                if(entry1.getValue().size() > highestValue) {
                    highestValueList = entry1.getValue();
                    highestValue = entry1.getValue().size();
                }
            }

            List<Customer> customers = multiKeyMap.get(Integer.parseInt(customerId), dayDate);

            List<Long> periods = new ArrayList<>();
            long overlapStart = Long.MIN_VALUE; // Maximum start time
            long overlapEnd = Long.MAX_VALUE;

            for(Customer customer : highestValueList) {
                String callId = customer.getCallId();
                for(Customer customer1 : customers) {
                    if(callId.equals(customer1.getCallId())) {
                        long start = customer1.getStartTimestamp();
                        long end = customer1.getEndTimestamp();
                        if(overlapStart < start) {
                            overlapStart = start;
                        }
                        if(overlapEnd > end) {
                            overlapEnd = end;
                        }
                    }
                }
            }

            if(highestValueList.size() > 0) {
                CustomerInfo customerInfo = new CustomerInfo();
                customerInfo.setCustomerId(Integer.parseInt(customerId));
                customerInfo.setDate(dayDate);
                customerInfo.setTimestamp((overlapStart + overlapEnd)/ 2);
                customerInfo.setMaxConcurrentCalls(highestValue);
                List<String> ids = new ArrayList<>();
                for(Customer valueSet : highestValueList ) {
                    ids.add(valueSet.getCallId());
                }

                customerInfo.setCallIds(ids);

                if(results.getResults() != null) {
                    results.getResults().add(customerInfo);
                }
                else {
                    results.setResults(new ArrayList<>());
                    results.getResults().add(customerInfo);
                }
            }

        }

        //restTemplate.postForEntity("https://candidate.hubteam.com/candidateTest/v3/problem/result?userKey=5831429e466fe642ea1bacd0b5bf", results, Results.class);
        return results;
    }

    public static boolean datesOverlap(long start1, long end2, long start2, long end1) {
        return start1 <= end2 && start2 <= end1;
    }

}
