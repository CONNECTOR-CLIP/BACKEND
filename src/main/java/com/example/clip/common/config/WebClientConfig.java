package com.example.clip.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// Python AI 서버 통신에 사용하는 WebClient Bean 설정
@Configuration
public class WebClientConfig {

    // 응답 바디 버퍼 한도를 10MB로 설정 — 대용량 논문 데이터 수신 대비
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
