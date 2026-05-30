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

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryAliasController {

    private final SearchService searchService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<String> saveHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SearchHistoryRequestDto requestDto) {
        String userId = resolveUserId(token, requestDto.getUserId());
        searchService.saveHistory(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("검색어 저장 완료");
    }

    @GetMapping
    public ResponseEntity<List<SearchHistoryResponseDto>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(searchService.getHistory(resolveUserId(token, userId)));
    }

    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }
}
