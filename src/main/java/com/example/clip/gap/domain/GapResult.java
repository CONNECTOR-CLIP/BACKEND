package com.example.clip.gap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "\"Insight\"")
public class GapResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Long insightId;

    @Column(name = "gap_content", nullable = false, columnDefinition = "TEXT")
    private String gapContent;

    @Column(name = "roadmap_id", nullable = false)
    private Long roadmapId;

    public GapResult(String gapContent, Long roadmapId) {
        this.gapContent = gapContent;
        this.roadmapId = roadmapId;
    }
}
