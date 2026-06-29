package com.wxw.cloud.tools;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public final class CookieUtils {

    static final Logger logger = LoggerFactory.getLogger(CookieUtils.class);

    private CookieUtils() {
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        return getCookieValue(request, cookieName, false);
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName, boolean isDecoder) {
        Cookie[] cookieList = request.getCookies();
        if (cookieList == null || cookieName == null) {
            return null;
        }
        try {
            for (Cookie cookie : cookieList) {
                if (cookie.getName().equals(cookieName)) {
                    return isDecoder ? URLDecoder.decode(cookie.getValue(), "UTF-8") : cookie.getValue();
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Cookie Decode Error.", e);
        }
        return null;
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName, String encodeString) {
        Cookie[] cookieList = request.getCookies();
        if (cookieList == null || cookieName == null) {
            return null;
        }
        try {
            for (Cookie cookie : cookieList) {
                if (cookie.getName().equals(cookieName)) {
                    return URLDecoder.decode(cookie.getValue(), encodeString);
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Cookie Decode Error.", e);
        }
        return null;
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                 String cookieName, String cookieValue, String encodeString) {
        setCookie(request, response, cookieName, cookieValue, null, encodeString, null);
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                 String cookieName, String cookieValue, Integer cookieMaxAge) {
        setCookie(request, response, cookieName, cookieValue, cookieMaxAge, null, null);
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                 String cookieName, String cookieValue, Integer cookieMaxAge,
                                 String encodeString) {
        setCookie(request, response, cookieName, cookieValue, cookieMaxAge, encodeString, null);
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                 String cookieName, String cookieValue, Integer cookieMaxAge,
                                 String encodeString, Boolean httpOnly) {
        try {
            if (StringUtils.isBlank(encodeString)) {
                encodeString = "utf-8";
            }

            if (cookieValue == null) {
                cookieValue = "";
            } else {
                cookieValue = URLEncoder.encode(cookieValue, encodeString);
            }

            Cookie cookie = new Cookie(cookieName, cookieValue);
            if (cookieMaxAge != null && cookieMaxAge > 0) {
                cookie.setMaxAge(cookieMaxAge);
            }

            String domainName = request == null ? null : getDomainName(request);
            if (StringUtils.isNotBlank(domainName)
                    && !"localhost".equalsIgnoreCase(domainName)
                    && !domainName.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                cookie.setDomain(domainName);
            }
            cookie.setPath("/");

            if (httpOnly != null) {
                cookie.setHttpOnly(httpOnly);
            }
            response.addCookie(cookie);
        } catch (Exception e) {
            logger.error("Cookie Encode Error.", e);
        }
    }

    private static String getDomainName(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (StringUtils.isBlank(serverName)) {
            return "";
        }

        serverName = serverName.toLowerCase();
        if ("localhost".equals(serverName) || serverName.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return serverName;
        }

        String[] domains = serverName.split("\\.");
        int len = domains.length;
        String domainName;
        if (len > 3) {
            domainName = domains[len - 3] + "." + domains[len - 2] + "." + domains[len - 1];
        } else if (len <= 3 && len > 1) {
            domainName = domains[len - 2] + "." + domains[len - 1];
        } else {
            domainName = serverName;
        }

        if (domainName.indexOf(":") > 0) {
            domainName = domainName.split("\\:")[0];
        }
        return domainName;
    }
}
