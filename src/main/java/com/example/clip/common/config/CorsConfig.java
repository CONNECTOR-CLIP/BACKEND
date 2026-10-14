package com.example.clip.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 전역 CORS 설정 — 프론트엔드 개발 환경에서 모든 오리진 허용
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // 모든 경로에 대해 CORS를 허용, 운영 환경에서는 allowedOriginPatterns를 제한할 것
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
