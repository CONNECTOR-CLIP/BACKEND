package com.example.clip.search.domain;

import com.example.clip.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 검색 기록 엔티티 — PostgreSQL "History" 테이블과 매핑, 소프트 삭제 방식 사용
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "\"History\"")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // isVisible=false로 설정해 숨김 처리 (실제 삭제 없이 소프트 삭제)
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "roadmap_id", nullable = false)
    private Long roadmapId;

    public SearchHistory(User user, String keyword, Long roadmapId) {
        this.user = user;
        this.keyword = keyword;
        this.isVisible = true;
        this.roadmapId = roadmapId;
    }
}
