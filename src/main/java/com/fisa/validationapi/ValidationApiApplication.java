package com.fisa.validationapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ValidationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationApiApplication.class, args);
    }

}
