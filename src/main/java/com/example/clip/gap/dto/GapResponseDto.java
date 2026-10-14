package com.example.clip.gap.dto;

import com.example.clip.gap.domain.GapResult;
import lombok.Getter;
import lombok.Setter;

// 갭 분석 응답 DTO — GapResult 엔티티를 클라이언트 응답 형식으로 변환
@Getter
@Setter
public class GapResponseDto {
    private Long insightId;
    private String gapContent;
    private Long roadmapId;

    public GapResponseDto(GapResult gapResult) {
        this.insightId = gapResult.getInsightId();
        this.gapContent = gapResult.getGapContent();
        this.roadmapId = gapResult.getRoadmapId();
    }
}
