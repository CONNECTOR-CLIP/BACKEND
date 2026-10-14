package com.example.clip.search.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 마인드맵 생성 요청 DTO — 선택한 논문 ID 목록과 검색 쿼리 포함
@Getter
@Setter
public class MindmapRequestDto {
    private List<String> paperIds; // 선택한 논문 ID 목록
    private String query;
}
