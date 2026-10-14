package com.example.clip.gap.controller;

import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.service.GapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 특정 로드맵에 대한 갭 분석 결과 목록 조회 API
@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
public class ResultController {

    private final GapService gapService;

    // GET /api/result?roadmapId={id} — roadmapId에 해당하는 갭 분석 결과 목록 반환 (최신순)
    @GetMapping
    public ResponseEntity<List<GapResponseDto>> getResults(
            @RequestParam(defaultValue = "0") Long roadmapId) {
        return ResponseEntity.ok(gapService.getByRoadmapId(roadmapId));
    }
}
