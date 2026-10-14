package com.example.clip.auth.dto;

import lombok.Getter;
import lombok.Setter;

// 로그인 성공 응답 DTO — 유저 아이디와 JWT 액세스 토큰 포함
@Getter
@Setter
public class AuthResponseDto {
    private String username;
    private String accessToken;

    public AuthResponseDto(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }
}