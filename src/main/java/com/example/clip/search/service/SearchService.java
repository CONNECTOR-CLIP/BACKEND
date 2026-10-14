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

// 검색 관련 핵심 로직 처리 — 마인드맵 생성, 검색 기록 관리, 트렌드 키워드 집계
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

    // paperIds → DB에서 논문 조회 → title/abstract 포함해 Python 마인드맵 API 호출 → 노드/엣지 반환
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

    // 검색어 저장 — roadmapId가 없으면 새 Roadmap 먼저 생성 후 연결
    @Transactional
    public void saveHistory(String userId, SearchHistoryRequestDto requestDto) {
        if (requestDto.getKeyword() == null || requestDto.getKeyword().isBlank()) {
            throw new IllegalArgumentException("keyword 값이 비어 있습니다.");
        }

        // 비회원이면 user=null로 저장
        User user = (userId != null) ? userRepository.findByUserId(userId).orElse(null) : null;

        Long roadmapId = requestDto.getRoadmapId();
        // roadmapId가 없으면 키워드를 제목으로 Roadmap 자동 생성
        if (roadmapId == null) {
            Roadmap roadmap = roadmapRepository.save(new Roadmap(requestDto.getKeyword(), user));
            roadmapId = roadmap.getRoadmapId();
        }

        searchHistoryRepository.save(new SearchHistory(user, requestDto.getKeyword(), roadmapId));
    }

    // 검색 기록 조회 — 로그인 유저면 본인 기록만, 비로그인이면 빈 목록 반환 (다른 사용자 기록 노출 방지)
    @Transactional(readOnly = true)
    public List<SearchHistoryResponseDto> getHistory(String userId) {
        if (userId != null) {
            User user = userRepository.findByUserId(userId).orElse(null);
            if (user != null) {
                return searchHistoryRepository.findByUserAndIsVisibleTrueOrderByCreatedAtDesc(user).stream()
                        .map(SearchHistoryResponseDto::new)
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    // 특정 검색 기록 숨김 처리 (소프트 삭제 — isVisible=false 설정)
    @Transactional
    public void deleteHistory(String userId, Long historyId) {
        searchHistoryRepository.findById(historyId).ifPresent(h -> {
            h.setIsVisible(false);
            searchHistoryRepository.save(h);
        });
    }

    // 유저의 전체 검색 기록 일괄 숨김 처리
    @Transactional
    public void clearHistory(String userId) {
        if (userId == null) return;
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) return;
        searchHistoryRepository.findByUser(user).forEach(h -> {
            h.setIsVisible(false);
            searchHistoryRepository.save(h);
        });
    }

    // 전체 검색 기록에서 키워드 빈도 집계 후 상위 8개 반환 (홈 화면 트렌드 키워드용)
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
