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
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryAliasController {

    private final SearchService searchService;
    private final JwtUtil jwtUtil;

    // POST /api/history — 검색 기록 저장, 생성된 id 반환
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SearchHistoryRequestDto requestDto) {
        String userId = resolveUserId(token, requestDto.getUserId());
        Long id = searchService.saveHistory(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", String.valueOf(id)));
    }

    // GET /api/history — 검색 기록 목록
    @GetMapping
    public ResponseEntity<List<SearchHistoryResponseDto>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(searchService.getHistory(resolveUserId(token, userId)));
    }

    // DELETE /api/history/{id} — 검색 기록 단건 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        searchService.deleteHistory(id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/history — 검색 기록 전체 삭제 (유저 식별 시 해당 유저 기록)
    @DeleteMapping
    public ResponseEntity<Void> deleteAllHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        searchService.deleteAllHistory(resolveUserId(token, userId));
        return ResponseEntity.ok().build();
    }

    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }
}
