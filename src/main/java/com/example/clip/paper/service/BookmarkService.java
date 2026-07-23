package com.example.clip.paper.service;

import com.example.clip.paper.domain.GapBookmark;
import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.domain.PaperBookmark;
import com.example.clip.paper.repository.GapBookmarkRepository;
import com.example.clip.paper.repository.PaperBookmarkRepository;
import com.example.clip.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final PaperBookmarkRepository paperBookmarkRepository;
    private final GapBookmarkRepository gapBookmarkRepository;
    private final PaperRepository paperRepository;

    // ---- 논문 북마크 ----
    @Transactional
    public Long addPaperBookmark(String userId, Map<String, Object> payload) {
        String paperId = str(payload, "paperId", "id", "arxiv_id");
        if (paperId == null || paperId.isBlank()) {
            throw new IllegalArgumentException("paperId가 필요합니다.");
        }
        if (paperBookmarkRepository.existsByUserIdAndPaperId(userId, paperId)) {
            throw new IllegalStateException("이미 북마크된 논문입니다.");
        }

        String title = str(payload, "title");
        String author = str(payload, "author", "submitter");
        String category = str(payload, "category", "privaryCategory", "primary_category");
        // 페이로드에 없으면 Paper 테이블에서 보충
        if (title == null || author == null || category == null) {
            Paper p = paperRepository.findById(paperId).orElse(null);
            if (p != null) {
                if (title == null) title = p.getTitle();
                if (author == null) author = p.getAuthor();
                if (category == null) category = p.getPrivaryCategory();
            }
        }

        PaperBookmark saved = paperBookmarkRepository.save(
                new PaperBookmark(userId, paperId, title, author, category));
        return saved.getBookmarkId();
    }

    @Transactional
    public void removePaperBookmark(String userId, String paperId) {
        paperBookmarkRepository.deleteByUserIdAndPaperId(userId, paperId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPaperBookmarks(String userId) {
        return paperBookmarkRepository.findByUserIdOrderByBookmarkIdDesc(userId).stream()
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("paperId", b.getPaperId());
                    m.put("title", b.getTitle());
                    m.put("author", b.getAuthor());
                    m.put("category", b.getCategory());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ---- GAP 북마크 ----
    @Transactional
    public Long addGapBookmark(String userId, Map<String, Object> payload) {
        String gapId = str(payload, "gapId", "id");
        if (gapId == null || gapId.isBlank()) {
            throw new IllegalArgumentException("gapId가 필요합니다.");
        }
        if (gapBookmarkRepository.existsByUserIdAndGapId(userId, gapId)) {
            throw new IllegalStateException("이미 북마크된 항목입니다.");
        }

        String title = str(payload, "title");
        String description = str(payload, "description");

        GapBookmark saved = gapBookmarkRepository.save(
                new GapBookmark(userId, gapId, title, description));
        return saved.getBookmarkId();
    }

    @Transactional
    public void removeGapBookmark(String userId, String gapId) {
        gapBookmarkRepository.deleteByUserIdAndGapId(userId, gapId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGapBookmarks(String userId) {
        return gapBookmarkRepository.findByUserIdOrderByBookmarkIdDesc(userId).stream()
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("gapId", b.getGapId());
                    m.put("title", b.getTitle());
                    m.put("description", b.getDescription());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // 여러 후보 키 중 처음으로 값이 있는 것을 문자열로 반환
    private String str(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof List<?> list && !list.isEmpty()) {
                return list.get(0).toString();
            }
            if (v != null && !v.toString().isBlank()) {
                return v.toString();
            }
        }
        return null;
    }
}
