package com.qshield.vam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.vam", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.vam.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.vam.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSVamApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-VAM");
        SpringApplication.run(QSVamApplication.class, args);
    }
}
