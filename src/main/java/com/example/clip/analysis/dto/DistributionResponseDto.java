package com.example.clip.analysis.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class DistributionResponseDto {
    private List<CategoryCountDto> distribution;
    private int total;

    public DistributionResponseDto(List<CategoryCountDto> distribution) {
        this.distribution = distribution;
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
            this.percentage = total > 0 ? Math.round((double) count / total * 1000.0) / 10.0 : 0;
        }
    }
}
