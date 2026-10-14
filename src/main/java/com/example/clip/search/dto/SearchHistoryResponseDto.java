package com.example.clip.search.dto;

import com.example.clip.search.domain.SearchHistory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 검색 기록 응답 DTO — SearchHistory 엔티티에서 클라이언트에 필요한 필드만 추출
@Getter
@Setter
public class SearchHistoryResponseDto {
    private Long historyId;
    private String keyword;
    private Boolean isVisible;
    private LocalDateTime createdAt;

    public SearchHistoryResponseDto(SearchHistory history) {
        this.historyId = history.getHistoryId();
        this.keyword = history.getKeyword();
        this.isVisible = history.getIsVisible();
        this.createdAt = history.getCreatedAt();
    }
}
