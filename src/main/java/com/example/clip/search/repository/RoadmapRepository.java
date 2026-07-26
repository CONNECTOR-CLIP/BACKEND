package com.example.clip.search.repository;

import com.example.clip.auth.domain.User;
import com.example.clip.search.domain.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    List<Roadmap> findByUser(User user);
    void deleteByUser(User user);
}
