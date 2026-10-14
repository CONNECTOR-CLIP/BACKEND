package com.example.clip.paper.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 갭 분석 결과 북마크 엔티티 — 저장 시각을 직접 관리 (JPA Auditing 미사용)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gap_bookmark")
public class GapBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public GapBookmark(String userId, String title, String content, String keyword) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.keyword = keyword;
        this.createdAt = LocalDateTime.now();
    }
}
