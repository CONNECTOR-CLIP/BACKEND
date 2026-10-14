package com.example.clip.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// 로그인 요청 DTO — 아이디/비밀번호 빈 값 검증 포함
@Getter
@Setter
public class LoginRequestDto {
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String userId;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String password;
}