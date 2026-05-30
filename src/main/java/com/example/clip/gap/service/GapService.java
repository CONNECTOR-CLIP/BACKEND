package com.example.clip.gap.service;

import com.example.clip.gap.domain.GapResult;
import com.example.clip.gap.dto.GapRequestDto;
import com.example.clip.gap.dto.GapResponseDto;
import com.example.clip.gap.repository.GapResultRepository;
import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.repository.PaperRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GapService {

    private final GapResultRepository gapResultRepository;
    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.api.base-url}")
    private String pythonBaseUrl;

    // POST /api/gap
    // paperIds → DB에서 abstract 조회 → Python 갭분석 → Insight 저장
    @Transactional
    public GapResponseDto analyzeGap(GapRequestDto requestDto) {
        if (requestDto.getPaperIds() == null || requestDto.getPaperIds().isEmpty()) {
            throw new IllegalArgumentException("선택한 논문이 없습니다.");
        }

        // paper_id 목록으로 DB에서 논문 조회
        List<Paper> papers = paperRepository.findAllById(requestDto.getPaperIds());

        if (papers.isEmpty()) {
            throw new IllegalArgumentException("선택한 논문을 찾을 수 없습니다.");
        }

        // Python에 보낼 형식으로 변환
        List<Map<String, Object>> paperData = papers.stream()
                .map(p -> Map.<String, Object>of(
                        "paper_id", p.getPaperId(),
                        "title",    p.getTitle() != null ? p.getTitle() : p.getPaperId(),
                        "abstract", p.getAbstracts() != null ? p.getAbstracts() : ""
                ))
                .collect(Collectors.toList());

        Map<String, Object> payload = Map.of("papers", paperData);

        // Python API 호출
        Map<String, Object> pythonResult = webClient.post()
                .uri(pythonBaseUrl + "/api/gap")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (pythonResult == null) {
            pythonResult = Collections.emptyMap();
        }

        String gapContent = (String) pythonResult.getOrDefault("gap_content", pythonResult.toString());
        Long roadmapId = requestDto.getRoadmapId() != null ? requestDto.getRoadmapId() : 0L;

        GapResult gapResult = new GapResult(gapContent, roadmapId);
        gapResultRepository.save(gapResult);

        return new GapResponseDto(gapResult);
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
