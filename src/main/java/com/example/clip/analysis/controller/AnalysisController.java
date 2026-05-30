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

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final GapService gapService;

    // ⑤ POST /api/analysis/distribution
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

    // POST /api/analysis — 프론트 analyzeGap 별칭
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
