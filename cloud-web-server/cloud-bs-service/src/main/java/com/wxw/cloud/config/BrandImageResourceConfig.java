package com.wxw.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class BrandImageResourceConfig implements WebMvcConfigurer {

    @Value("${app.brand-image.local-dir:${user.dir}/brand-images}")
    private String localDir;

    @Value("${app.sku-image.local-dir:${user.dir}/sku-images}")
    private String skuImageDir;

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
        registry.addResourceHandler("/brand/images/**").addResourceLocations(location);

        File skuDir = new File(skuImageDir).getAbsoluteFile();
        if (!skuDir.exists()) {
            skuDir.mkdirs();
        }
        String skuLocation = skuDir.toURI().toString();
        if (!skuLocation.endsWith("/")) {
            skuLocation = skuLocation + "/";
        }
        registry.addResourceHandler("/sku/images/**").addResourceLocations(skuLocation);
    }
}
