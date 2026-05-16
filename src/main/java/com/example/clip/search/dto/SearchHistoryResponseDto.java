package com.example.clip.search.dto;

import com.example.clip.search.domain.SearchHistory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
