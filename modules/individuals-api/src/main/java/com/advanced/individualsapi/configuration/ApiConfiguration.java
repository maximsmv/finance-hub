package com.advanced.individualsapi.configuration;

import com.advanced.contract.ApiClient;
import com.advanced.contract.api.TransactionRestControllerV1Api;
import com.advanced.contract.api.UserRestControllerV1Api;
import com.advanced.contract.api.WalletRestControllerV1Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfiguration {

    @Bean
    public UserRestControllerV1Api userApi(
            @Value("${person-service.base-url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return new UserRestControllerV1Api(apiClient);
    }

    @Bean
    public TransactionRestControllerV1Api transactionClient(
            @Value("${transaction-service.base-url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return new TransactionRestControllerV1Api(apiClient);
    }

    @Bean
    public WalletRestControllerV1Api walletClient(
            @Value("${transaction-service.base-url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return new WalletRestControllerV1Api(apiClient);
    }

}
