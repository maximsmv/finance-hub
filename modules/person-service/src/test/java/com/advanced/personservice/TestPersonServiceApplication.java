package com.advanced.personservice;

import org.springframework.boot.SpringApplication;

public class TestPersonServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(PersonServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
