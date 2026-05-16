package com.example.clip.rbactest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class RBACTestController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/admin")
    public String testAdminOnly() {
        return "admin";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/user")
    public String testUserOnly() {
        return "user";
    }
}
