package com.example.clip.rbactest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RBAC(역할 기반 접근 제어) 동작 확인용 테스트 컨트롤러
@RequestMapping("/api")
@RestController
public class RBACTestController {

    // ADMIN 권한만 접근 가능한 엔드포인트
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/admin")
    public String testAdminOnly() {
        return "admin";
    }

    // ADMIN 권한만 접근 가능 (user 경로지만 실제로는 ADMIN 전용 — 네이밍 주의)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/user")
    public String testUserOnly() {
        return "user";
    }
}
