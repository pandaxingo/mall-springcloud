package com.wxw.cloud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author twx
 * @create:   2026-5-16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wxw.filter")
public class FilterProperties {

    private List<String> allowPaths;
}
