package com.example.clip.paper.repository;

import com.example.clip.paper.domain.GapBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GapBookmarkRepository extends JpaRepository<GapBookmark, Long> {
    List<GapBookmark> findByUserIdOrderByBookmarkIdDesc(String userId);
    boolean existsByUserIdAndGapId(String userId, String gapId);
    void deleteByUserIdAndGapId(String userId, String gapId);
    void deleteByUserId(String userId);
}
