package com.example.clip.auth.controller;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.repository.UserRepository;
import com.example.clip.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 로그인된 유저의 프로필 조회/수정/탈퇴 API 처리
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // GET /api/users/information — 토큰에서 유저 식별 후 프로필 정보 반환
    @GetMapping("/information")
    public ResponseEntity<Map<String, Object>> getInformation(
            @RequestHeader(value = "Authorization", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "profileImageUrl", user.getProfileImageUrl() == null ? "" : user.getProfileImageUrl()
        ));
    }

    // PATCH /api/users/nickname — 닉네임 변경, 빈 값이면 400 반환
    @PatchMapping("/nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> payload) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String nickname = payload.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "nickname 값이 필요합니다."));
        }
        user.setNickname(nickname);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("nickname", user.getNickname()));
    }

    // DELETE /api/users/account — 회원 탈퇴, DB에서 유저 레코드 삭제
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/users/password — 비밀번호 변경 (메서드명 오해 주의: 실제로는 변경 처리)
    @DeleteMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> payload) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String password = payload.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // BCrypt로 인코딩 후 저장
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // Authorization 헤더에서 JWT를 파싱해 User 엔티티 반환, 토큰 없거나 유저 미존재 시 null
    private User resolveUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String userId = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        return userRepository.findByUserId(userId).orElse(null);
    }
}
