package com.advanced.individualsapi.configuration;

import com.advanced.contract.ApiClient;
import com.advanced.contract.api.UserRestControllerV1Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonServiceApiConfiguration {

    @Bean
    public UserRestControllerV1Api userApi() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8081");
        return new UserRestControllerV1Api(apiClient);
    }

}
