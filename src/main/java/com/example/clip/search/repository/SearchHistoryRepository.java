package com.example.clip.search.repository;

import com.example.clip.auth.domain.User;
import com.example.clip.search.domain.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 검색 기록 조회용 JPA Repository — isVisible 필터를 통해 소프트 삭제된 항목 제외
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // 특정 유저의 활성(isVisible=true) 검색 기록 최신순 조회
    List<SearchHistory> findByUserAndIsVisibleTrueOrderByCreatedAtDesc(User user);
    // 전체 활성 검색 기록 최신순 조회 (트렌드 키워드 집계에 사용)
    List<SearchHistory> findByIsVisibleTrueOrderByCreatedAtDesc();
    // 검색 기록 전체 삭제(clearHistory) 시 isVisible 일괄 변경을 위해 사용
    List<SearchHistory> findByUser(User user);
}
