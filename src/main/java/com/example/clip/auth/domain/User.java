package com.example.clip.auth.domain;

import com.example.clip.common.domain.TimeStamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "\"User\"") // postgreSQL에서의 대문자
public class User extends TimeStamped {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "user_role", nullable = true)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    public User(String userId, String nickname, String password, String email, UserRole userRole) {
        this.userId = userId;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.userRole = userRole;
    }
}