package com.example.clip.auth.repository;

import com.example.clip.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 유저 조회/중복 확인을 위한 JPA Repository
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // userId로 유저 단건 조회
    Optional<User> findByUserId(String userId);

    // 회원가입 시 아이디 중복 확인
    boolean existsByUserId(String userId);

    // 회원가입 시 이메일 중복 확인
    boolean existsByEmail(String email);
}
