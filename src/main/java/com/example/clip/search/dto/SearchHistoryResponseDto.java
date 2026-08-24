package com.example.clip.search.dto;

import com.example.clip.search.domain.SearchHistory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SearchHistoryResponseDto {
    private String id;
    private String query;
    private LocalDateTime searchedAt;

    public SearchHistoryResponseDto(SearchHistory history) {
        this.id = history.getHistoryId() != null ? String.valueOf(history.getHistoryId()) : null;
        this.query = history.getKeyword();
        this.searchedAt = history.getCreatedAt();
    }
}
