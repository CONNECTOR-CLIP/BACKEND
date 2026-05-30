package com.example.clip.gap.controller;

import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.service.GapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
public class ResultController {

    private final GapService gapService;

    @GetMapping
    public ResponseEntity<List<GapResponseDto>> getResults(
            @RequestParam(defaultValue = "0") Long roadmapId) {
        return ResponseEntity.ok(gapService.getByRoadmapId(roadmapId));
    }
}
