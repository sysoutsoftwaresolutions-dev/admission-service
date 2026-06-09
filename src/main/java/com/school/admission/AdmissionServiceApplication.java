package com.school.admission;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Admission Service microservice.
 */
@SpringBootApplication
@EnableMongock
public class AdmissionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdmissionServiceApplication.class, args);
    }
}
