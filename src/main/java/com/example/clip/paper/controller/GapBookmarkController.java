package com.example.clip.paper.controller;

import com.example.clip.auth.util.JwtUtil;
import com.example.clip.paper.domain.GapBookmark;
import com.example.clip.paper.repository.GapBookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks/gap")
@RequiredArgsConstructor
public class GapBookmarkController {

    private final GapBookmarkRepository gapBookmarkRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getGapBookmarks(
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Map<String, Object>> result = gapBookmarkRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(b -> Map.<String, Object>of(
                        "id", b.getId(),
                        "title", b.getTitle() != null ? b.getTitle() : "",
                        "content", b.getContent() != null ? b.getContent() : "",
                        "keyword", b.getKeyword() != null ? b.getKeyword() : "",
                        "createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addGapBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String title = (String) payload.getOrDefault("title", "");
        String content = (String) payload.getOrDefault("content", "");
        String keyword = (String) payload.getOrDefault("keyword", "");

        GapBookmark bookmark = new GapBookmark(userId, title, content, keyword);
        gapBookmarkRepository.save(bookmark);

        return ResponseEntity.ok(Map.of(
                "id", bookmark.getId(),
                "title", title,
                "content", content,
                "keyword", keyword,
                "createdAt", bookmark.getCreatedAt().toString()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeGapBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        gapBookmarkRepository.deleteByIdAndUserId(id, userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        try {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
