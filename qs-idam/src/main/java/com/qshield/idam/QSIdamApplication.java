package com.qshield.idam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.idam", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.idam.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.idam.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSIdamApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-IDAM");
        SpringApplication.run(QSIdamApplication.class, args);
    }
}
