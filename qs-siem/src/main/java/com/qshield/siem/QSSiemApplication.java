package com.qshield.siem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.siem", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.siem.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.siem.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSSiemApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-SIEM");
        SpringApplication.run(QSSiemApplication.class, args);
    }
}
