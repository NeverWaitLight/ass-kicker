package com.github.waitlight.asskicker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling
public class AssKickerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssKickerApplication.class, args);
    }
}
