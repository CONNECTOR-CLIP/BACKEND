package com.example.clip.analysis.service;

import com.example.clip.analysis.dto.DistributionRequestDto;
import com.example.clip.analysis.dto.DistributionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 마인드맵 노드 데이터를 기반으로 카테고리별 논문 분포를 계산하는 서비스
@Service
@RequiredArgsConstructor
public class AnalysisService {

    // 마인드맵 nodes → label 기준으로 children 수 집계 → 점유율 계산 후 내림차순 정렬
    public DistributionResponseDto getDistribution(DistributionRequestDto requestDto) {
        List<Map<String, Object>> nodes = requestDto.getNodes();

        // label이 카테고리명, children 리스트의 크기가 해당 카테고리의 논문 수
        Map<String, Long> countMap = nodes.stream()
                .filter(node -> node.get("label") != null
                        && node.get("children") != null)
                .collect(Collectors.groupingBy(
                        node -> node.get("label").toString(),
                        Collectors.summingLong(node -> ((List<?>) node.get("children")).size())
                ));

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        // 논문 수 기준 내림차순 정렬
        List<DistributionResponseDto.CategoryCountDto> distribution = countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new DistributionResponseDto.CategoryCountDto(e.getKey(), e.getValue(), total))
                .collect(Collectors.toList());

        return new DistributionResponseDto(distribution);
    }
}
