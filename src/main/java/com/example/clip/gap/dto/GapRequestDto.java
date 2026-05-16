package com.example.clip.gap.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GapRequestDto {
    private List<String> paperIds; // 선택한 논문 ID 목록
    private Long roadmapId;
    private String userId;
}
