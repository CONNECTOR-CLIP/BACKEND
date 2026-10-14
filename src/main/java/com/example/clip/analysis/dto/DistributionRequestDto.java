package com.example.clip.analysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// 분포 분석 요청 DTO — 마인드맵 결과의 nodes 배열을 그대로 전달
@Getter
@Setter
public class DistributionRequestDto {
    private List<Map<String, Object>> nodes; // mindmap 결과의 nodes
}
