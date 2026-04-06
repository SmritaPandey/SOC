package com.qshield.xdr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qshield.xdr", "com.qshield.common"})
@EntityScan(basePackages = {"com.qshield.xdr.model", "com.qshield.common.audit"})
@EnableJpaRepositories(basePackages = {"com.qshield.xdr.repository", "com.qshield.common.audit"})
@EnableScheduling
public class QSXdrApplication {
    public static void main(String[] args) {
        System.setProperty("spring.application.name", "QS-XDR");
        SpringApplication.run(QSXdrApplication.class, args);
    }
}
