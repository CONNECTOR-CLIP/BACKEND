package com.example.clip.paper.repository;

import com.example.clip.paper.domain.Bookmark;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 논문 북마크 조회/삭제/중복 확인용 JPA Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserId(String userId);
    Optional<Bookmark> findByUserIdAndPaperId(String userId, String paperId);
    @Transactional
    void deleteByUserIdAndPaperId(String userId, String paperId);
    boolean existsByUserIdAndPaperId(String userId, String paperId);
}
