package com.example.clip.search.service;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.repository.UserRepository;
import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.repository.PaperRepository;
import com.example.clip.search.domain.Roadmap;
import com.example.clip.search.domain.SearchHistory;
import com.example.clip.search.dto.*;
import com.example.clip.search.repository.RoadmapRepository;
import com.example.clip.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final RoadmapRepository roadmapRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.api.base-url}")
    private String pythonBaseUrl;

    // POST /api/search/mindmap
    // paperIds → DB에서 abstract 조회 → Python으로 전달 → 노드/엣지 반환
    public MindmapResponseDto getMindmap(MindmapRequestDto requestDto) {
        if (requestDto.getPaperIds() == null || requestDto.getPaperIds().isEmpty()) {
            throw new IllegalArgumentException("선택한 논문이 없습니다.");
        }

        // paper_id 목록으로 DB에서 논문 조회
        List<Paper> papers = paperRepository.findAllById(requestDto.getPaperIds());

        if (papers.isEmpty()) {
            throw new IllegalArgumentException("선택한 논문을 찾을 수 없습니다.");
        }

        // Python에 보낼 형식으로 변환 (title + abstract)
        List<Map<String, Object>> paperData = papers.stream()
                .map(p -> Map.<String, Object>of(
                        "paper_id", p.getPaperId(),
                        "title",    p.getTitle() != null ? p.getTitle() : p.getPaperId(),
                        "abstract", p.getAbstracts() != null ? p.getAbstracts() : ""
                ))
                .collect(Collectors.toList());

        Map<String, Object> payload = Map.of(
                "query",  requestDto.getQuery() != null ? requestDto.getQuery() : "",
                "papers", paperData
        );

        Map<String, Object> result = webClient.post()
                .uri(pythonBaseUrl + "/api/search/mindmap")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (result == null) {
            result = Collections.emptyMap();
        }

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
        return new MindmapResponseDto(nodes, edges);
    }

    // POST /api/search/history
    @Transactional
    public void saveHistory(String userId, SearchHistoryRequestDto requestDto) {
        if (requestDto.getKeyword() == null || requestDto.getKeyword().isBlank()) {
            throw new IllegalArgumentException("keyword 값이 비어 있습니다.");
        }

        User user = (userId != null) ? userRepository.findByUserId(userId).orElse(null) : null;

        Long roadmapId = requestDto.getRoadmapId();
        if (roadmapId == null) {
            Roadmap roadmap = roadmapRepository.save(new Roadmap(requestDto.getKeyword(), user));
            roadmapId = roadmap.getRoadmapId();
        }

        searchHistoryRepository.save(new SearchHistory(user, requestDto.getKeyword(), roadmapId));
    }

    // GET /api/search/history
    @Transactional(readOnly = true)
    public List<SearchHistoryResponseDto> getHistory(String userId) {
        if (userId != null) {
            User user = userRepository.findByUserId(userId).orElse(null);
            if (user != null) {
                return searchHistoryRepository.findByUserOrderByCreatedAtDesc(user).stream()
                        .map(SearchHistoryResponseDto::new)
                        .collect(Collectors.toList());
            }
        }
        return searchHistoryRepository.findByIsVisibleTrueOrderByCreatedAtDesc().stream()
                .map(SearchHistoryResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getTrendKeywords() {
        return searchHistoryRepository.findByIsVisibleTrueOrderByCreatedAtDesc().stream()
                .collect(Collectors.groupingBy(
                        SearchHistory::getKeyword,
                        LinkedHashMap::new,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(8)
                .collect(Collectors.toList());
    }
}
