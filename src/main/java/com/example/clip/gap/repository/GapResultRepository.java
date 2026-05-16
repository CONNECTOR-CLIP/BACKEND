package com.example.clip.gap.repository;

import com.example.clip.gap.domain.GapResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GapResultRepository extends JpaRepository<GapResult, Long> {
    List<GapResult> findByRoadmapIdOrderByInsightIdDesc(Long roadmapId);
}
