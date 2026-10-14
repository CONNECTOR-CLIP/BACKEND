package com.example.clip.search.domain;

import com.example.clip.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로드맵 엔티티 — 검색 기록 저장 시 자동 생성되며 SearchHistory와 연결
@Entity
@Getter
@NoArgsConstructor
@Table(name = "\"Roadmap\"")
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmap_id")
    private Long roadmapId;

    @Column(name = "roadmap_title", nullable = false, length = 100)
    private String roadmapTitle;

    // 로드맵 소유 유저, 비회원의 경우 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Roadmap(String roadmapTitle, User user) {
        this.roadmapTitle = roadmapTitle;
        this.user = user;
    }
}
