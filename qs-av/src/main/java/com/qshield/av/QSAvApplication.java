package com.qshield.av;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.av", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.av.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.av.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSAvApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-AV");
        SpringApplication.run(QSAvApplication.class, args);
    }
}
