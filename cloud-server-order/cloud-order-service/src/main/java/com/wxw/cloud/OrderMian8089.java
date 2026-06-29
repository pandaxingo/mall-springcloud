package com.wxw.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@EnableDiscoveryClient
@SpringBootApplication
public class OrderMian8089 {
    public static void main(String[] args) {
        SpringApplication.run(OrderMian8089.class,args);
    }
}
