package com.qshield.edr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.edr", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.edr.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.edr.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSEdrApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-EDR");
        SpringApplication.run(QSEdrApplication.class, args);
    }
}
