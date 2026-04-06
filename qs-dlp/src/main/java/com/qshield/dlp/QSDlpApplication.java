package com.qshield.dlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.dlp", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.dlp.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.dlp.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSDlpApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-DLP");
        SpringApplication.run(QSDlpApplication.class, args);
    }
}
