package com.wxw.cloud.filter;

import cn.hutool.json.JSONUtil;
import com.wxw.cloud.config.FilterProperties;
import com.wxw.cloud.config.JwtProperties;
import com.wxw.cloud.tools.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE;

@Component
@Slf4j
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class LoginFilter implements GlobalFilter, Ordered {

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private FilterProperties filterProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        List<String> allowPaths = this.filterProperties.getAllowPaths();
        Map<String, Object> attributes = exchange.getAttributes();
        attributes.put(PRESERVE_HOST_HEADER_ATTRIBUTE, true);

        String requestUrl = String.valueOf(attributes.get(GATEWAY_ORIGINAL_REQUEST_URL_ATTR));
        log.info("request url: {}, allow paths: {}", requestUrl, JSONUtil.toJsonStr(allowPaths));
        for (String allowPath : allowPaths) {
            if (StringUtils.contains(requestUrl, allowPath)) {
                return chain.filter(exchange);
            }
        }

        String token = getToken(request);
        log.info("token => {}", token);
        if (StringUtils.isBlank(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
        } catch (Exception e) {
            log.info("parse token failed: {}", e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private String getToken(ServerHttpRequest request) {
        String cookieName = this.jwtProperties.getCookieName();
        String token = request.getQueryParams().getFirst(cookieName);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }

        token = request.getHeaders().getFirst(cookieName);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }

        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
