package com.qshield.soar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.soar", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.soar.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.soar.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSSoarApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-SOAR");
        SpringApplication.run(QSSoarApplication.class, args);
    }
}
