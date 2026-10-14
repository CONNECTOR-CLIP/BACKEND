package com.example.clip.paper.dto;

import lombok.Getter;
import lombok.Setter;

// 논문 검색 요청 DTO — 키워드, 카테고리, 페이지 정보 포함
@Getter
@Setter
public class SearchRequestDto {
    private String keyword;
    private String category;
    private int page = 0;
    private int size = 20;
}
