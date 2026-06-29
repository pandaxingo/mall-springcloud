package com.wxw.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@EnableEurekaClient
@SpringBootApplication
public class BS2020Main {
    public static void main(String[] args) {
        SpringApplication.run(BS2020Main.class, args);
    }
}
