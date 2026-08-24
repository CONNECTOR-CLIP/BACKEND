package com.example.clip.paper.controller;

import com.example.clip.auth.util.JwtUtil;
import com.example.clip.paper.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtUtil jwtUtil;

    // GET /api/bookmarks — 논문 북마크 목록
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBookmarks(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(bookmarkService.getPaperBookmarks(resolveUserId(token, userId)));
    }

    // POST /api/bookmarks/paper — 논문 북마크 추가
    @PostMapping("/paper")
    public ResponseEntity<Map<String, Object>> addPaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        try {
            String userId = resolveUserId(token, str(payload, "userId"));
            Long bookmarkId = bookmarkService.addPaperBookmark(userId, payload);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("bookmarkId", String.valueOf(bookmarkId)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("논문 북마크 추가 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "북마크 저장에 실패했습니다."));
        }
    }

    // DELETE /api/bookmarks/paper/{id} — 논문 북마크 삭제 (id = paperId)
    @DeleteMapping("/paper/{id}")
    public ResponseEntity<Void> removePaperBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId,
            @PathVariable String id) {
        bookmarkService.removePaperBookmark(resolveUserId(token, userId), id);
        return ResponseEntity.ok().build();
    }

    // GET /api/bookmarks/gap — GAP 북마크 목록
    @GetMapping("/gap")
    public ResponseEntity<List<Map<String, Object>>> getGapBookmarks(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(bookmarkService.getGapBookmarks(resolveUserId(token, userId)));
    }

    // POST /api/bookmarks/gap — GAP 북마크 추가
    @PostMapping("/gap")
    public ResponseEntity<Map<String, Object>> addGapBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        try {
            String userId = resolveUserId(token, str(payload, "userId"));
            Long bookmarkId = bookmarkService.addGapBookmark(userId, payload);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("bookmarkId", String.valueOf(bookmarkId)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("GAP 북마크 추가 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "북마크 저장에 실패했습니다."));
        }
    }

    // DELETE /api/bookmarks/gap/{id} — GAP 북마크 삭제 (id = gapId)
    @DeleteMapping("/gap/{id}")
    public ResponseEntity<Void> removeGapBookmark(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String userId,
            @PathVariable String id) {
        bookmarkService.removeGapBookmark(resolveUserId(token, userId), id);
        return ResponseEntity.ok().build();
    }

    private String resolveUserId(String token, String fallbackUserId) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(token.replace("Bearer ", ""));
        }
        return (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : null;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
