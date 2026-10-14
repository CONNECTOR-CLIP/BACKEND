package com.example.clip.analysis.dto;

import lombok.Getter;

import java.util.List;

// 분포 분석 응답 DTO — 카테고리별 논문 수, 비율, 전체 합계 포함
@Getter
public class DistributionResponseDto {
    private List<CategoryCountDto> distribution;
    private int total;

    public DistributionResponseDto(List<CategoryCountDto> distribution) {
        this.distribution = distribution;
        // 전체 논문 수는 각 카테고리의 count 합산
        this.total = distribution.stream().mapToInt(c -> (int) c.getCount()).sum();
    }

    @Getter
    public static class CategoryCountDto {
        private String category;
        private long count;
        private double percentage;

        public CategoryCountDto(String category, long count, long total) {
            this.category = category;
            this.count = count;
            // 소수점 1자리 퍼센트 (반올림 처리)
            this.percentage = total > 0 ? Math.round((double) count / total * 1000.0) / 10.0 : 0;
        }
    }
}
