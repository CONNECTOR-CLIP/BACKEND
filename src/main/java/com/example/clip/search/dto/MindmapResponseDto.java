package com.example.clip.search.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// 마인드맵 응답 DTO — Python AI가 생성한 노드와 엣지 데이터를 클라이언트로 전달
@Getter
@Setter
public class MindmapResponseDto {
    private List<Map<String, Object>> nodes; // 클러스터 노드
    private List<Map<String, Object>> edges; // 노드 간 연결

    public MindmapResponseDto(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }
}
