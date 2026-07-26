package com.example.clip.auth.controller;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.repository.UserRepository;
import com.example.clip.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
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
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String userId = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        return userRepository.findByUserId(userId).orElse(null);
    }
}
