package com.example.clip.search.repository;

import com.example.clip.auth.domain.User;
import com.example.clip.search.domain.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserOrderByCreatedAtDesc(User user);
    List<SearchHistory> findByIsVisibleTrueOrderByCreatedAtDesc();

    void deleteByUser(User user);
}
