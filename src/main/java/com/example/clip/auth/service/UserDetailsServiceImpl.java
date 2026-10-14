package com.example.clip.auth.service;

import com.example.clip.auth.domain.PrincipalDetails;
import com.example.clip.auth.domain.User;
import com.example.clip.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security 인증 흐름에서 userId로 UserDetails를 로드하는 서비스
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security의 UserDetailsService 인터페이스의 필수 구현 메서드
    // AuthenticationManager가 로그인 처리 시 내부적으로 이 메서드를 호출
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " ));

        System.out.println("조회된 사용자의 역할: " + user.getUserRole());

        return new PrincipalDetails(user);
    }
}