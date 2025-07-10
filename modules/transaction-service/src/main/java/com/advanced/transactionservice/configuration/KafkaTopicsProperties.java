package com.advanced.transactionservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {
    private String depositRequested;
    private String withdrawalRequested;
    private String depositCompleted;
    private String withdrawalCompleted;
    private String withdrawalFailed;
}
