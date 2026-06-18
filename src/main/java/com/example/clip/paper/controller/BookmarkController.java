package com.example.clip.paper.controller;

import com.example.clip.auth.util.JwtUtil;
import com.example.clip.paper.domain.Bookmark;
import com.example.clip.paper.repository.BookmarkRepository;
import com.example.clip.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkRepository bookmarkRepository;
    private final PaperRepository paperRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBookmarks(
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Map<String, Object>> result = bookmarkRepository.findByUserId(userId).stream()
                .map(b -> Map.<String, Object>of(
                        "id", b.getId(),
                        "paperId", b.getPaperId(),
                        "title", b.getTitle() != null ? b.getTitle() : b.getPaperId(),
                        "category", b.getCategory() != null ? b.getCategory() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/paper")
    public ResponseEntity<Map<String, Object>> addPaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String paperId = (String) payload.get("paperId");
        if (paperId == null || paperId.isBlank())
            return ResponseEntity.badRequest().build();

        if (bookmarkRepository.existsByUserIdAndPaperId(userId, paperId))
            return ResponseEntity.ok(payload);

        String title = (String) payload.getOrDefault("title", paperId);
        String category = (String) payload.getOrDefault("category", "");

        // Paper 테이블에서 제목/카테고리 보완
        if (title.equals(paperId)) {
            paperRepository.findById(paperId).ifPresent(p -> {
                payload.put("title", p.getTitle() != null ? p.getTitle() : paperId);
                payload.put("category", p.getPrivaryCategory() != null ? p.getPrivaryCategory() : "");
            });
            title = (String) payload.getOrDefault("title", paperId);
            category = (String) payload.getOrDefault("category", "");
        }

        Bookmark bookmark = new Bookmark(paperId, userId, title, category);
        bookmarkRepository.save(bookmark);

        return ResponseEntity.ok(Map.of(
                "id", bookmark.getId(),
                "paperId", paperId,
                "title", title,
                "category", category
        ));
    }

    @DeleteMapping("/paper/{id}")
    public ResponseEntity<Void> removePaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        bookmarkRepository.deleteByUserIdAndPaperId(userId, id);
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
