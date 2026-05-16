package com.example.clip.search.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MindmapRequestDto {
    private List<String> paperIds; // 선택한 논문 ID 목록
    private String query;
}
