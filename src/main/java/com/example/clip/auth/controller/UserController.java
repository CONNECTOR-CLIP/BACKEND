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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "profileImageUrl", user.getProfileImageUrl() == null ? "" : user.getProfileImageUrl()
        ));
    }

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
