package com.example.clip.paper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBookmarks() {
        return ResponseEntity.ok(new ArrayList<>());
    }

    @PostMapping("/paper")
    public ResponseEntity<Map<String, Object>> addPaperBookmark(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(payload);
    }

    @DeleteMapping("/paper/{id}")
    public ResponseEntity<Void> removePaperBookmark(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }
}
