package com.example.clip.analysis.service;

import com.example.clip.analysis.dto.DistributionRequestDto;
import com.example.clip.analysis.dto.DistributionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    // ⑤ POST /api/analysis/distribution — mindmap nodes → 카테고리별 점유율 계산
    public DistributionResponseDto getDistribution(DistributionRequestDto requestDto) {
        List<Map<String, Object>> nodes = requestDto.getNodes();

        // category 기준으로 논문 수 집계
        Map<String, Long> countMap = nodes.stream()
                .filter(node -> node.get("category") != null)
                .collect(Collectors.groupingBy(
                        node -> node.get("category").toString(),
                        Collectors.counting()
                ));

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<DistributionResponseDto.CategoryCountDto> distribution = countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new DistributionResponseDto.CategoryCountDto(e.getKey(), e.getValue(), total))
                .collect(Collectors.toList());

        return new DistributionResponseDto(distribution);
    }
}
