package com.example.clip.search.repository;

import com.example.clip.search.domain.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;

// 로드맵 저장/조회용 JPA Repository (현재는 기본 CRUD만 사용)
public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
}
