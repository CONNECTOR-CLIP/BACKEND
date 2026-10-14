package com.example.clip.gap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 갭 분석(연구 공백) 결과 엔티티 — PostgreSQL "Insight" 테이블과 매핑
@Entity
@Getter
@NoArgsConstructor
@Table(name = "\"Insight\"")
public class GapResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Long insightId;

    // Python AI가 생성한 갭 분석 텍스트 (길이 제한 없음)
    @Column(name = "gap_content", nullable = false, columnDefinition = "TEXT")
    private String gapContent;

    @Column(name = "roadmap_id", nullable = false)
    private Long roadmapId;

    public GapResult(String gapContent, Long roadmapId) {
        this.gapContent = gapContent;
        this.roadmapId = roadmapId;
    }
}
