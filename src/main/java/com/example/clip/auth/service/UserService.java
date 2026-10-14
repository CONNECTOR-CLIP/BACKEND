package com.example.clip.auth.service;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.domain.UserRole;
import com.example.clip.auth.dto.SignUpRequestDto;
import com.example.clip.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원가입 비즈니스 로직 처리
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 아이디/이메일 중복 확인 후 신규 유저 저장
    @Transactional
    public void registerUser(SignUpRequestDto signUpRequestDto) {
        // 아이디 중복이면 예외 발생 (AuthController에서 400으로 변환)
        if (userRepository.existsByUserId(signUpRequestDto.getUserId())) {
            throw new IllegalArgumentException("UserID 사용자 계정이 사용중입니다.");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email 사용자 이메일이 사용중입니다.");
        }

        // 비밀번호는 BCrypt 인코딩 후 저장
        User user = new User(
                signUpRequestDto.getUserId(),
                signUpRequestDto.getNickname(),
                passwordEncoder.encode(signUpRequestDto.getPassword()),
                signUpRequestDto.getEmail(),
                UserRole.ROLE_USER
        );

        userRepository.save(user);
    }
}