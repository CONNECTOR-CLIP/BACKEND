package com.example.clip.paper.repository;

import com.example.clip.paper.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserId(String userId);
    Optional<Bookmark> findByUserIdAndPaperId(String userId, String paperId);
    void deleteByUserIdAndPaperId(String userId, String paperId);
    boolean existsByUserIdAndPaperId(String userId, String paperId);
}
