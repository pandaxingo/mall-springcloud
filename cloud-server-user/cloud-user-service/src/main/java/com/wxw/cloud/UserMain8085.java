package com.wxw.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@EnableDiscoveryClient
@SpringBootApplication
public class UserMain8085 {
    public static void main(String[] args) {
        SpringApplication.run(UserMain8085.class, args);
    }
}
