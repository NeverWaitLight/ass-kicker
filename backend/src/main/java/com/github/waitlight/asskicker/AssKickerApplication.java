package com.github.waitlight.asskicker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class AssKickerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AssKickerApplication.class, args);
  }
}
