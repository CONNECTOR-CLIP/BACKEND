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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 갭 분석 핵심 로직 처리 — RDS/OpenSearch 논문 조회 후 Python AI API 호출
@Slf4j
@Service
@RequiredArgsConstructor
public class GapService {

    private final GapResultRepository gapResultRepository;
    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.futurework.api.base-url}")
    private String pythonBaseUrl;

    // 비동기 갭 분석 작업: Python job_id → roadmapId (완료 시 Insight 저장에 사용)
    private final Map<String, Long> jobRoadmapIds = new ConcurrentHashMap<>();
    // 이미 Insight로 저장한 작업: Python job_id → insightId (폴링마다 중복 저장 방지)
    private final Map<String, Long> jobInsightIds = new ConcurrentHashMap<>();

    // paperIds → RDS 조회 → 없으면 OpenSearch(메타데이터)에서 가져와 RDS 저장 → Python 갭분석 → Insight 저장
    @Transactional
    public GapResponseDto analyzeGap(GapRequestDto requestDto) {
        Map<String, Object> payload = buildPythonPayload(requestDto);

        // 4. Python FutureWork API 호출 — 최대 30분 대기 (논문 수에 따라 파이프라인 지연)
        Map<String, Object> pythonResult;
        try {
            pythonResult = webClient.post()
                    .uri(pythonBaseUrl + "/api/gap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofMinutes(30));
        } catch (Exception e) {
            log.error("Python FutureWork API 호출 실패: {}", e.getMessage());
            pythonResult = Collections.emptyMap();
        }

        if (pythonResult == null) pythonResult = Collections.emptyMap();

        // gap_content 키로 텍스트 추출, 없으면 전체 응답을 문자열로 저장
        String gapContent = (String) pythonResult.getOrDefault("gap_content", pythonResult.toString());
        Long roadmapId = requestDto.getRoadmapId() != null ? requestDto.getRoadmapId() : 0L;

        GapResult gapResult = new GapResult(gapContent, roadmapId);
        gapResultRepository.save(gapResult);

        return new GapResponseDto(gapResult);
    }

    // POST /api/gap/jobs — Python에 비동기 갭 분석 작업을 시작시키고 job_id를 반환
    @Transactional
    public Map<String, Object> startGapJob(GapRequestDto requestDto) {
        Map<String, Object> payload = buildPythonPayload(requestDto);

        Map<String, Object> started = webClient.post()
                .uri(pythonBaseUrl + "/api/gap/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(30));

        if (started == null || started.get("job_id") == null) {
            throw new IllegalStateException("Python 갭 분석 작업을 시작하지 못했습니다.");
        }
        String jobId = (String) started.get("job_id");
        jobRoadmapIds.put(jobId, requestDto.getRoadmapId() != null ? requestDto.getRoadmapId() : 0L);
        return Map.of("jobId", jobId, "status", started.getOrDefault("status", "queued"));
    }

    // GET /api/gap/jobs/{jobId} — Python 작업의 단계별 진행 상황을 그대로 전달하고, 완료되면 Insight로 한 번 저장
    @Transactional
    public Map<String, Object> getGapJob(String jobId) {
        Map<String, Object> job;
        try {
            job = webClient.get()
                    .uri(pythonBaseUrl + "/api/gap/jobs/" + jobId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException.NotFound e) {
            throw new IllegalArgumentException("갭 분석 작업을 찾을 수 없습니다: " + jobId);
        }
        if (job == null) {
            throw new IllegalArgumentException("갭 분석 작업을 찾을 수 없습니다: " + jobId);
        }

        Map<String, Object> response = new HashMap<>(job);
        String gapContent = (String) job.get("gap_content");
        if ("completed".equals(job.get("status")) && gapContent != null) {
            Long insightId = jobInsightIds.computeIfAbsent(jobId, id -> {
                Long roadmapId = jobRoadmapIds.getOrDefault(id, 0L);
                return gapResultRepository.save(new GapResult(gapContent, roadmapId)).getInsightId();
            });
            response.put("insightId", insightId);
        }
        return response;
    }

    // 선택 논문을 RDS(없으면 OpenSearch 백필)에서 찾아 Python 갭 분석 요청 형식으로 변환
    private Map<String, Object> buildPythonPayload(GapRequestDto requestDto) {
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

        // 3. Python에 보낼 형식으로 변환 (paper_id, title, abstract)
        List<Map<String, Object>> paperData = papers.stream()
                .map(p -> Map.<String, Object>of(
                        "paper_id", p.getPaperId(),
                        "title",    p.getTitle() != null ? p.getTitle() : p.getPaperId(),
                        "abstract", p.getAbstracts() != null ? p.getAbstracts() : ""
                ))
                .collect(Collectors.toList());

        return Map.of("papers", paperData);
    }

    // RDS에 논문이 없을 때 OpenSearch(로컬 9200)에서 arxiv_id로 조회 후 RDS에 백필
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
                        // OpenSearch 응답의 _source 필드에서 논문 정보 추출
                        Map<String, Object> source = (Map<String, Object>) hitList.get(0).get("_source");

                        Paper paper = new Paper();
                        paper.setPaperId(paperId);
                        paper.setTitle((String) source.getOrDefault("title", paperId));
                        paper.setAbstracts((String) source.getOrDefault("abstract", ""));
                        paper.setPrivaryCategory((String) source.getOrDefault("arxiv_primary_category", ""));
                        paper.setCreatedDate((String) source.getOrDefault("published", ""));

                        // authors가 리스트일 경우 콤마로 합쳐서 저장
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

    // 갭 분석 제안을 Python에 전달해 논문 초안 텍스트 생성
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

    // insightId로 갭 분석 결과 단건 조회, 없으면 예외 발생
    @Transactional(readOnly = true)
    public GapResponseDto getById(Long insightId) {
        return gapResultRepository.findById(insightId)
                .map(GapResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다: " + insightId));
    }

    // roadmapId에 연결된 갭 분석 결과 목록 조회
    @Transactional(readOnly = true)
    public List<GapResponseDto> getByRoadmapId(Long roadmapId) {
        return gapResultRepository.findByRoadmapIdOrderByInsightIdDesc(roadmapId).stream()
                .map(GapResponseDto::new)
                .collect(Collectors.toList());
    }
}
