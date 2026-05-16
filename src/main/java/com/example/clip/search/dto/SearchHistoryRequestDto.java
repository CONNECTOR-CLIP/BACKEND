package com.example.clip.search.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchHistoryRequestDto {
    private String keyword;   // History 테이블의 keyword 컬럼
    private Long roadmapId;   // History 테이블의 roadmap_id (FK, NOTNULL)
    private String userId;    // 테스트용 (나중에 JWT로 대체)
}
