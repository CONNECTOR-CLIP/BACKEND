package com.example.clip.gap.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 갭 분석 요청 DTO — 사용자가 선택한 논문 ID 목록과 로드맵 ID 포함
@Getter
@Setter
public class GapRequestDto {
    private List<String> paperIds; // 선택한 논문 ID 목록
    private Long roadmapId;
    private String userId;
}
