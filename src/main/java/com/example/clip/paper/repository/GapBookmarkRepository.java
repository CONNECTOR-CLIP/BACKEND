package com.example.clip.paper.repository;

import com.example.clip.paper.domain.GapBookmark;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 갭 분석 북마크 조회/삭제용 JPA Repository
public interface GapBookmarkRepository extends JpaRepository<GapBookmark, Long> {
    List<GapBookmark> findByUserIdOrderByCreatedAtDesc(String userId);
    @Transactional
    void deleteByIdAndUserId(Long id, String userId);
}
