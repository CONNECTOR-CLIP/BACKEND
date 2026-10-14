package com.example.clip.gap.controller;

import com.example.clip.gap.dto.GapRequestDto;
import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.service.GapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gap")
@RequiredArgsConstructor
public class GapController {

    private final GapService gapService;

    // ③ POST /api/gap
    @PostMapping
    public ResponseEntity<GapResponseDto> analyzeGap(@RequestBody GapRequestDto requestDto) {
        try {
            GapResponseDto response = gapService.analyzeGap(requestDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("GAP 분석 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // POST /api/gap/refresh — 프론트 호환 별칭
    @PostMapping("/refresh")
    public ResponseEntity<GapResponseDto> refreshGap(@RequestBody GapRequestDto requestDto) {
        return analyzeGap(requestDto);
    }

    // POST /api/gap/jobs — 갭 분석을 비동기 작업으로 시작하고 jobId 반환 (단계별 진행 상황 표시용)
    @PostMapping("/jobs")
    public ResponseEntity<Map<String, Object>> startGapJob(@RequestBody GapRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(gapService.startGapJob(requestDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("GAP 분석 작업 시작 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "AI 서버에 분석을 요청하지 못했습니다."));
        }
    }

    // GET /api/gap/jobs/{jobId} — 갭 분석 작업의 단계별 진행 상황 조회, 완료 시 insightId 포함
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getGapJob(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(gapService.getGapJob(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("GAP 분석 작업 조회 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "AI 서버에서 진행 상황을 가져오지 못했습니다."));
        }
    }

    // GET /api/gap/{id}
    @GetMapping("/{id}")
    public ResponseEntity<GapResponseDto> getGap(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gapService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
