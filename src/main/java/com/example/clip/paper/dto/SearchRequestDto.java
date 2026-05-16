package com.example.clip.paper.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {
    private String keyword;
    private String category;
    private int page = 0;
    private int size = 20;
}
