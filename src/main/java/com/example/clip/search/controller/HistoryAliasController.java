package com.example.clip.search.controller;

import com.example.clip.auth.util.JwtUtil;
import com.example.clip.search.dto.SearchHistoryRequestDto;
import com.example.clip.search.dto.SearchHistoryResponseDto;
import com.example.clip.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/search/history와 동일한 기능을 /api/history 경로로 제공하는 별칭 컨트롤러
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryAliasController {

    private final SearchService searchService;
    private final JwtUtil jwtUtil;

    // POST /api/history — 검색어 저장
    @PostMapping
    public ResponseEntity<String> saveHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SearchHistoryRequestDto requestDto) {
        String userId = resolveUserId(token, requestDto.getUserId());
        searchService.saveHistory(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("검색어 저장 완료");
    }

    // GET /api/history — 검색 기록 조회
    @GetMapping
    public ResponseEntity<List<SearchHistoryResponseDto>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(searchService.getHistory(resolveUserId(token, userId)));
    }

    // DELETE /api/history/{id} — 특정 검색 기록 숨김 처리 (실제 삭제가 아닌 isVisible=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        searchService.deleteHistory(resolveUserId(token, null), id);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/history — 로그인 유저의 검색 기록 전체 숨김 처리
    @DeleteMapping
    public ResponseEntity<Void> clearHistory(
            @RequestHeader(value = "Authorization", required = false) String token) {
        searchService.clearHistory(resolveUserId(token, null));
        return ResponseEntity.noContent().build();
    }

    // JWT 우선, 없으면 쿼리파라미터 userId를 폴백으로 사용
    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }
}
