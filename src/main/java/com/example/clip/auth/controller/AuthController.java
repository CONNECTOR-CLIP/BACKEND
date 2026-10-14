package com.example.clip.auth.controller;

// ... 기존 임포트 ...

import com.example.clip.auth.dto.AuthResponseDto;
import com.example.clip.auth.dto.LoginRequestDto;
import com.example.clip.auth.dto.SignUpRequestDto;
import com.example.clip.auth.service.UserService;
import com.example.clip.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// 회원가입/로그인 등 인증 관련 API 처리
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    // POST /api/auth/signup — 유효성 검사 후 회원 등록, 중복 시 400 반환
    @PostMapping("/auth/signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        try {
            userService.registerUser(signUpRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원 가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원 가입 중 오류가 발생했습니다.");
        }
    }

    // POST /api/auth/login — 자격증명 검증 후 JWT 액세스 토큰 발급
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        try {
            // Spring Security AuthenticationManager로 아이디/비밀번호 검증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getUserId(),
                            loginRequestDto.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(new AuthResponseDto(userDetails.getUsername(), accessToken));
        } catch (BadCredentialsException e) {
            // 아이디 또는 비밀번호 불일치 시 401 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDto(null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponseDto(null, null));
        }
    }

    // POST /api/auth/logout — JWT는 서버에서 상태를 유지하지 않으므로 클라이언트 측 토큰 삭제로 처리
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    // POST /api/auth/social/login — 소셜 로그인 (미구현)
    @PostMapping("/auth/social/login")
    public ResponseEntity<AuthResponseDto> socialLogin() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new AuthResponseDto(null, null));
    }

    // POST /api/auth/findId — 아이디 찾기 (미구현)
    @PostMapping("/auth/findId")
    public ResponseEntity<String> findId() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("아이디 찾기는 아직 구현되지 않았습니다.");
    }

    // POST /api/auth/findpassword — 비밀번호 찾기 (미구현)
    @PostMapping("/auth/findpassword")
    public ResponseEntity<String> findPassword() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("비밀번호 찾기는 아직 구현되지 않았습니다.");
    }
}
