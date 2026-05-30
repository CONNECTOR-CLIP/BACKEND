package com.example.clip.search.controller;

import com.example.clip.auth.util.JwtUtil;
import com.example.clip.search.dto.*;
import com.example.clip.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final JwtUtil jwtUtil;

    // POST /api/search/mindmap
    @PostMapping("/mindmap")
    public ResponseEntity<MindmapResponseDto> getMindmap(@RequestBody MindmapRequestDto requestDto) {
        try {
            MindmapResponseDto response = searchService.getMindmap(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("mindmap 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // POST /api/search/history
    @PostMapping("/history")
    public ResponseEntity<String> saveHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SearchHistoryRequestDto requestDto) {
        try {
            String userId = resolveUserId(token, requestDto.getUserId());
            searchService.saveHistory(userId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body("검색어 저장 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("검색어 저장 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류: " + e.getMessage());
        }
    }

    // GET /api/search/history
    @GetMapping("/history")
    public ResponseEntity<List<SearchHistoryResponseDto>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        try {
            String resolvedUserId = resolveUserId(token, userId);
            List<SearchHistoryResponseDto> response = searchService.getHistory(resolvedUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/search/recent
    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistoryResponseDto>> getRecentSearchWords(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return getHistory(token, userId);
    }

    // GET /api/search/trend
    @GetMapping("/trend")
    public ResponseEntity<List<String>> getTrendKeywords() {
        return ResponseEntity.ok(searchService.getTrendKeywords());
    }

    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }
}
