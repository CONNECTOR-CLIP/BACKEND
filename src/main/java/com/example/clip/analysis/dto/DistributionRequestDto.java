package com.example.clip.analysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DistributionRequestDto {
    private List<Map<String, Object>> nodes; // mindmap 결과의 nodes
}
