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

// 논문 검색 관련 API 처리 — 마인드맵 생성, 검색 기록 저장/조회, 트렌드 키워드 제공
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final JwtUtil jwtUtil;

    // POST /api/search/mindmap — 선택한 논문들로 Python 마인드맵 API 호출 후 노드/엣지 반환
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

    // POST /api/search/history — 검색어와 roadmapId를 History 테이블에 저장
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

    // GET /api/search/history — isVisible=true인 검색 기록 조회 (유저 로그인 시 본인 기록만)
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

    // GET /api/search/recent — /api/search/history와 동일 (프론트 호환 별칭)
    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistoryResponseDto>> getRecentSearchWords(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return getHistory(token, userId);
    }

    // GET /api/search/trend — 전체 검색 기록에서 빈도 상위 8개 키워드 반환
    @GetMapping("/trend")
    public ResponseEntity<List<String>> getTrendKeywords() {
        return ResponseEntity.ok(searchService.getTrendKeywords());
    }

    // JWT 우선, 없으면 요청 파라미터 userId를 폴백으로 사용
    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }
}
