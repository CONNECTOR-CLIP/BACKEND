package com.example.clip.gap.dto;

import com.example.clip.gap.domain.GapResult;
import lombok.Getter;
import lombok.Setter;

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
