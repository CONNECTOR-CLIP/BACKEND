package com.example.clip.analysis.controller;

import com.example.clip.analysis.dto.DistributionRequestDto;
import com.example.clip.analysis.dto.DistributionResponseDto;
import com.example.clip.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AnalysisController {

    private final AnalysisService analysisService;

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
}
