package com.example.clip.paper.repository;

import com.example.clip.paper.domain.PaperBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperBookmarkRepository extends JpaRepository<PaperBookmark, Long> {
    List<PaperBookmark> findByUserIdOrderByBookmarkIdDesc(String userId);
    boolean existsByUserIdAndPaperId(String userId, String paperId);
    void deleteByUserIdAndPaperId(String userId, String paperId);
    void deleteByUserId(String userId);
}
