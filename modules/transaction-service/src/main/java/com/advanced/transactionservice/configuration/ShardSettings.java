package com.advanced.transactionservice.configuration;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ShardSettings {
    private int count;
    private Map<String, ShardingSphereConfiguration.DataSourceProperties> datasources = new HashMap<>();
}
