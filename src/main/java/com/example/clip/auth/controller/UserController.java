package com.example.clip.auth.controller;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.repository.UserRepository;
import com.example.clip.auth.service.UserService;
import com.example.clip.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/information")
    public ResponseEntity<Map<String, Object>> getInformation(
            @RequestHeader(value = "Authorization", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증이 필요합니다."));
        }
        // null 값을 담기 위해 Map.of 대신 HashMap 사용 (Map.of는 null 미허용)
        Map<String, Object> response = new HashMap<>();
        response.put("nickname", user.getNickname());
        response.put("email", user.getEmail());
        response.put("userId", user.getUserId());
        response.put("profileImage", user.getProfileImageUrl());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> payload) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증이 필요합니다."));
        }
        String nickname = payload.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "nickname 값이 필요합니다."));
        }
        if (nickname.equals(user.getNickname())) {
            return ResponseEntity.ok(Map.of("nickname", user.getNickname()));
        }
        // 다른 유저가 이미 쓰는 닉네임이면 409
        if (userRepository.existsByNickname(nickname)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 사용 중인 닉네임입니다."));
        }
        try {
            user.setNickname(nickname);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("nickname", user.getNickname()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "닉네임 변경에 실패했습니다."));
        }
    }

    // 비밀번호 변경 (현재 비번 검증 후 변경).
    @DeleteMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> payload) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증이 필요합니다."));
        }
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "현재 비밀번호와 새 비밀번호를 입력해주세요."));
        }
        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        }
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "비밀번호 변경에 실패했습니다."));
        }
    }
    
    // JWT는 서버 상태가 없어 토큰 자체를 무효화할 수 없으나, 유저 삭제 시
    // 이후 요청은 resolveUser에서 유저를 못 찾아 401이 되어 사실상 무효화됨.
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증이 필요합니다."));
        }
        try {
            userService.deleteAccount(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("회원 탈퇴 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "회원 탈퇴에 실패했습니다."));
        }
    }

    private User resolveUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String userId = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        return userRepository.findByUserId(userId).orElse(null);
    }
}
