package com.example.clip.gap.repository;

import com.example.clip.gap.domain.GapResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 갭 분석 결과(Insight) 조회용 JPA Repository
public interface GapResultRepository extends JpaRepository<GapResult, Long> {
    // 특정 로드맵의 갭 분석 결과 목록을 최신순으로 반환
    List<GapResult> findByRoadmapIdOrderByInsightIdDesc(Long roadmapId);
}
