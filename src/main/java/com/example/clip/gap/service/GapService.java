package com.example.clip.gap.service;

import com.example.clip.gap.domain.GapResult;
import com.example.clip.gap.dto.DraftRequestDto;
import com.example.clip.gap.dto.GapRequestDto;
import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.repository.GapResultRepository;
import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GapService {

    private final GapResultRepository gapResultRepository;
    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.futurework.api.base-url}")
    private String pythonBaseUrl;

    // POST /api/gap
    // paperIds → RDS 조회 → 없으면 OpenSearch(메타데이터)에서 가져와 RDS 저장 → Python 갭분석 → Insight 저장
    @Transactional
    public GapResponseDto analyzeGap(GapRequestDto requestDto) {
        if (requestDto.getPaperIds() == null || requestDto.getPaperIds().isEmpty()) {
            throw new IllegalArgumentException("선택한 논문이 없습니다.");
        }

        // 1. RDS에서 먼저 찾기
        List<Paper> papers = paperRepository.findAllById(requestDto.getPaperIds());

        // 2. RDS에 없으면 OpenSearch(메타데이터)에서 가져와서 저장
        if (papers.isEmpty()) {
            papers = fetchFromOpenSearchAndSave(requestDto.getPaperIds());
        }

        if (papers.isEmpty()) {
            throw new IllegalArgumentException("선택한 논문을 찾을 수 없습니다.");
        }

        // 3. Python에 보낼 형식으로 변환
        List<Map<String, Object>> paperData = papers.stream()
                .map(p -> Map.<String, Object>of(
                        "paper_id", p.getPaperId(),
                        "title",    p.getTitle() != null ? p.getTitle() : p.getPaperId(),
                        "abstract", p.getAbstracts() != null ? p.getAbstracts() : ""
                ))
                .collect(Collectors.toList());

        Map<String, Object> payload = Map.of("papers", paperData);

        // 4. Python FutureWork API 호출
        Map<String, Object> pythonResult;
        try {
            pythonResult = webClient.post()
                    .uri(pythonBaseUrl + "/api/gap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofMinutes(10));
        } catch (Exception e) {
            log.error("Python FutureWork API 호출 실패: {}", e.getMessage());
            pythonResult = Collections.emptyMap();
        }

        if (pythonResult == null) pythonResult = Collections.emptyMap();

        String gapContent = (String) pythonResult.getOrDefault("gap_content", pythonResult.toString());
        Long roadmapId = requestDto.getRoadmapId() != null ? requestDto.getRoadmapId() : 0L;

        GapResult gapResult = new GapResult(gapContent, roadmapId);
        gapResultRepository.save(gapResult);

        return new GapResponseDto(gapResult);
    }

    // OpenSearch(메타데이터)에서 논문 가져와서 RDS에 저장
    @SuppressWarnings("unchecked")
    private List<Paper> fetchFromOpenSearchAndSave(List<String> paperIds) {
        List<Paper> savedPapers = new ArrayList<>();
        for (String paperId : paperIds) {
            try {
                Map<String, Object> result = webClient.get()
                        .uri("http://localhost:9200/arxiv_papers/_search?q=arxiv_id:" + paperId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                if (result != null) {
                    Map<String, Object> hits = (Map<String, Object>) result.get("hits");
                    List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");

                    if (hitList != null && !hitList.isEmpty()) {
                        Map<String, Object> source = (Map<String, Object>) hitList.get(0).get("_source");

                        Paper paper = new Paper();
                        paper.setPaperId(paperId);
                        paper.setTitle((String) source.getOrDefault("title", paperId));
                        paper.setAbstracts((String) source.getOrDefault("abstract", ""));
                        paper.setPrivaryCategory((String) source.getOrDefault("arxiv_primary_category", ""));
                        paper.setCreatedDate((String) source.getOrDefault("published", ""));

                        Object authorsObj = source.get("authors");
                        if (authorsObj instanceof List) {
                            paper.setAuthor(String.join(", ", (List<String>) authorsObj));
                        }

                        paperRepository.save(paper);
                        savedPapers.add(paper);
                        log.info("OpenSearch(메타데이터)에서 논문 RDS 저장 완료: {}", paperId);
                    }
                }
            } catch (Exception e) {
                log.error("OpenSearch 논문 조회 실패: {} - {}", paperId, e.getMessage());
            }
        }
        return savedPapers;
    }

    public Map<String, Object> generateDraft(DraftRequestDto requestDto) {
        Map<String, Object> payload = Map.of("proposal", requestDto.getProposal());
        Map<String, Object> result;
        try {
            result = webClient.post()
                    .uri(pythonBaseUrl + "/api/gap/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Python 논문 초안 생성 API 호출 실패: {}", e.getMessage());
            result = Collections.emptyMap();
        }
        return result != null ? result : Collections.emptyMap();
    }

    @Transactional(readOnly = true)
    public GapResponseDto getById(Long insightId) {
        return gapResultRepository.findById(insightId)
                .map(GapResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다: " + insightId));
    }

    @Transactional(readOnly = true)
    public List<GapResponseDto> getByRoadmapId(Long roadmapId) {
        return gapResultRepository.findByRoadmapIdOrderByInsightIdDesc(roadmapId).stream()
                .map(GapResponseDto::new)
                .collect(Collectors.toList());
    }
}
