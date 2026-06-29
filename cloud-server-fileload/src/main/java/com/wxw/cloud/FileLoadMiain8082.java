package com.wxw.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@EnableEurekaClient
@SpringBootApplication
public class FileLoadMiain8082 {
    public static void main(String[] args) {
        SpringApplication.run(FileLoadMiain8082.class, args);
    }
}
