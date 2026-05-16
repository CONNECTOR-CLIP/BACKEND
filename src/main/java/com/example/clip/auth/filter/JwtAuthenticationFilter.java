package com.example.clip.auth.filter;

import com.example.clip.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // 회원가입과 로그인 경로는 이 필터를 거치지 않고 바로 통과시킵니다.
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. HTTP 요청 헤더에서 Authorization이름의 키의 값을 가져옴
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Authorization 헤더가 없거나 "Bearer "로 시작하지 않으면 JWT가 없는 요청으로 간주(미인증)하고 다음 필터로 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 접두사 (7자)를 제거하고 순수한 JWT 토큰을 추출
        jwt = authHeader.substring(7);

        try {
            // 4. JWT 토큰에서 사용자 이름(계정명 *Username)을 추출
            username = jwtUtil.extractUsername(jwt);
        } catch (RuntimeException e) { // JwtUtil에서 발생하는 JwtException(RuntimeException) 처리
            // JWT 토큰 파싱 또는 검증 중 오류가 발생하면 401 Unauthorized 에러 응답
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401
            response.getWriter().write(e.getMessage()); // 에러 메시지 반환 (디버깅용)
            return;
        }

        // 5. 사용자 이름이 존재하고, Spring Security Context에 아직 인증 정보가 없는 경우에만 인증 진행
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 6. UserDetailsService를 통해 사용자 계정(이름 *Username)을 기반으로 UserDetails 객체를 로드
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 7. 토큰이 유효한지 검증 (사용자 이름 일치, 토큰 만료 여부)
            if (jwtUtil.validateToken(jwt, userDetails)) {
                // 8. 토큰이 유효하면 Spring Security의 인증 토큰을 생성
                // UsernamePasswordAuthenticationToken은 인증 주체, 자격 증명, 권한을 포함
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // 인증된 사용자 주체 (UserDetails 객체)
                        null,        // 자격 증명 (비밀번호 등 - 여기서는 이미 토큰으로 인증되었으므로 null)
                        userDetails.getAuthorities() // 사용자의 권한 목록
                );

                // 9. 요청에 대한 웹 인증 세부 정보(IP 주소, 세션 ID 등)를 설정
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 10. SecurityContextHolder에 인증 토큰을 설정
                // 이렇게 설정하면 현재 요청에 대해 사용자가 인증되었음을 Spring Security에 알림
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // 11. 다음 필터 또는 서블릿으로 요청을 전달
        filterChain.doFilter(request, response);
    }
}