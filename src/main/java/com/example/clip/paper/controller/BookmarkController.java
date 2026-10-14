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

// 논문 북마크 추가/조회/삭제 API 처리
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkRepository bookmarkRepository;
    private final PaperRepository paperRepository;
    private final JwtUtil jwtUtil;

    // GET /api/bookmarks — 로그인 유저의 북마크 목록 반환
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

    // POST /api/bookmarks/paper — 논문 북마크 추가, 이미 존재하면 그대로 반환 (중복 방지)
    @PostMapping("/paper")
    public ResponseEntity<Map<String, Object>> addPaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String paperId = (String) payload.get("paperId");
        if (paperId == null || paperId.isBlank())
            return ResponseEntity.badRequest().build();

        // 이미 북마크된 논문이면 재저장 없이 기존 데이터 반환
        if (bookmarkRepository.existsByUserIdAndPaperId(userId, paperId))
            return ResponseEntity.ok(payload);

        String title = (String) payload.getOrDefault("title", paperId);
        String category = (String) payload.getOrDefault("category", "");

        // 요청에 title이 없으면 Paper 테이블에서 제목/카테고리 조회해서 보완
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

    // DELETE /api/bookmarks/paper/{id} — paperId 기준으로 북마크 삭제
    @DeleteMapping("/paper/{id}")
    public ResponseEntity<Void> removePaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id) {
        String userId = resolveUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        bookmarkRepository.deleteByUserIdAndPaperId(userId, id);
        return ResponseEntity.noContent().build();
    }

    // JWT에서 userId 추출, 토큰 없거나 파싱 실패 시 null 반환
    private String resolveUserId(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        try {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
