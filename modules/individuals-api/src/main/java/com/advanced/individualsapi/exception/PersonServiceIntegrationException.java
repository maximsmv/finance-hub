package com.advanced.individualsapi.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class PersonServiceIntegrationException extends RuntimeException {

    private final HttpStatusCode status;
    private final String body;

    public PersonServiceIntegrationException(HttpStatusCode status, String body) {
        super("Ошибка при обращении к person-service: " + status + " " + body);
        this.status = status;
        this.body = body;
    }

}
