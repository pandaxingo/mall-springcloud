package com.wxw.cloud.interceptor;

import com.wxw.cloud.config.JwtProperties;
import com.wxw.cloud.domain.UserInfo;
import com.wxw.cloud.tools.CookieUtils;
import com.wxw.cloud.tools.JwtUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author twx
 * @create:   2026-5-16
 */
@Component
@EnableConfigurationProperties(value = {JwtProperties.class})
public class LoginOrderInterceptor extends HandlerInterceptorAdapter {

    @Resource
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        if (token == null || token.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
            if (userInfo == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            THREAD_LOCAL.set(userInfo);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    public static UserInfo getUserinfo() {
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
