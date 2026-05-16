package com.example.clip.gap.controller;

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
@RequestMapping("/api/gap")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
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
}
