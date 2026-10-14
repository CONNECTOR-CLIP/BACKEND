package com.example.clip.analysis.controller;

import com.example.clip.analysis.dto.DistributionRequestDto;
import com.example.clip.analysis.dto.DistributionResponseDto;
import com.example.clip.analysis.service.AnalysisService;
import com.example.clip.gap.dto.GapRequestDto;
import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.service.GapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 논문 분포 분석 및 갭 분석 진입점 API 처리
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final GapService gapService;

    // POST /api/analysis/distribution — 마인드맵 노드를 받아 카테고리별 논문 수 및 점유율 계산
    @PostMapping("/distribution")
    public ResponseEntity<DistributionResponseDto> getDistribution(
            @RequestBody DistributionRequestDto requestDto) {
        try {
            DistributionResponseDto response = analysisService.getDistribution(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("distribution 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // POST /api/analysis — /api/gap과 동일한 갭 분석 처리, 프론트 호환 별칭 엔드포인트
    @PostMapping
    public ResponseEntity<GapResponseDto> analyzeGap(@RequestBody GapRequestDto requestDto) {
        try {
            GapResponseDto response = gapService.analyzeGap(requestDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("analysis gap 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
