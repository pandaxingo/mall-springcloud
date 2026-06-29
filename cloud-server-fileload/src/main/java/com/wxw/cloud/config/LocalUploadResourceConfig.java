package com.wxw.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class LocalUploadResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.local-dir:${user.dir}/upload-images}")
    private String localDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadDir = new File(localDir).getAbsoluteFile();
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        String location = uploadDir.toURI().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/images/**").addResourceLocations(location);
    }
}
