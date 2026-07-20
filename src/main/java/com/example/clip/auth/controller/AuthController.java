package com.example.clip.auth.controller;

// ... 기존 임포트 ...

import com.example.clip.auth.dto.AuthResponseDto;
import com.example.clip.auth.dto.LoginRequestDto;
import com.example.clip.auth.dto.MessageResponseDto;
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


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/auth/signup")
    public ResponseEntity<MessageResponseDto> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        try {
            userService.registerUser(signUpRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponseDto("회원 가입 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponseDto("회원 가입 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        try {
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDto(null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponseDto(null, null));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/social/login")
    public ResponseEntity<AuthResponseDto> socialLogin() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new AuthResponseDto(null, null));
    }

    @PostMapping("/auth/findId")
    public ResponseEntity<String> findId() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("아이디 찾기는 아직 구현되지 않았습니다.");
    }

    @PostMapping("/auth/findpassword")
    public ResponseEntity<String> findPassword() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("비밀번호 찾기는 아직 구현되지 않았습니다.");
    }
}
