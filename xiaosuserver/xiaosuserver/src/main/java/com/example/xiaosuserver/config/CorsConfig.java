package com.example.xiaosuserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    /**
     * 配置跨域请求过滤器
     * 允许前端应用与后端服务器进行通信
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许特定来源，而不是使用通配符
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://localhost");
        // 添加Android应用可能使用的来源
        config.addAllowedOrigin("http://47.97.65.168:8080");
        config.addAllowedOrigin("http://47.97.65.168");
        
        // 允许所有头信息
        config.addAllowedHeader("*");
        // 允许所有方法（GET, POST等）
        config.addAllowedMethod("*");
        // 允许发送Cookie
        config.setAllowCredentials(true);
        // 预检请求的有效期，单位为秒
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}