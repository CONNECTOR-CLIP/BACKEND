package com.example.clip.paper.repository;

import com.example.clip.paper.domain.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 논문 키워드 검색 및 카테고리 필터링용 JPA Repository
public interface PaperRepository extends JpaRepository<Paper, String> {

    // title, abstract, author 세 필드에 대해 대소문자 무시 키워드 검색
    @Query("SELECT p FROM Paper p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.abstracts) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Paper> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 주요 카테고리(privaryCategory) 기준 필터링
    Page<Paper> findByPrivaryCategory(String category, Pageable pageable);
}
