package com.example.clip.paper.service;

import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.dto.PaperResponseDto;
import com.example.clip.paper.dto.SearchRequestDto;
import com.example.clip.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.api.base-url}")
    private String pythonBaseUrl;

    // POST /api/search — Python 검색 API 호출
    public Map<String, Object> search(SearchRequestDto requestDto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("keyword", requestDto.getKeyword() != null ? requestDto.getKeyword() : "");
        payload.put("page", requestDto.getPage());
        payload.put("size", requestDto.getSize());
        if (requestDto.getCategory() != null && !requestDto.getCategory().isBlank()) {
            payload.put("category", requestDto.getCategory());
        }

        // FE 타임아웃(10초)보다 먼저 끊어서 message 있는 에러 응답을 내려주기 위한 제한
        Map<String, Object> result = webClient.post()
                .uri(pythonBaseUrl + "/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block(java.time.Duration.ofSeconds(9));

        return result != null ? result : Collections.emptyMap();
    }

    // POST /api/search — Python 응답을 FE 스펙({ topics: [{ id, label, papers }] })으로 정규화
    public Map<String, Object> searchTopics(SearchRequestDto requestDto) {
        Map<String, Object> raw = search(requestDto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("topics", normalizeTopics(raw, requestDto.getKeyword()));
        return response;
    }

    // GET /api/roadmap — 검색 결과를 FE 스펙({ keyword, topics: [{ id, label, papers }] })으로 정규화
    // 2-1(검색)과 달리 Python roadmap 트리의 의미있는 주제명(label)을 사용
    public Map<String, Object> roadmap(SearchRequestDto requestDto) {
        Map<String, Object> raw = search(requestDto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keyword", requestDto.getKeyword());
        response.put("topics", normalizeRoadmapTopics(raw, requestDto.getKeyword()));
        return response;
    }

    // Python roadmap 트리(roots→intermediate_nodes→children)를 topics 구조로 변환.
    // 주제 노드의 label을 그대로 쓰고, children의 paper_id로 papers 리스트에서 논문 상세를 join.
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeRoadmapTopics(Map<String, Object> raw, String keyword) {
        if (raw == null || !(raw.get("roadmap") instanceof Map<?, ?> roadmap)) {
            // 로드맵 트리가 없으면 카테고리 그룹 방식으로 폴백
            return normalizeTopics(raw, keyword);
        }

        // paper_id → 정규화된 논문 상세 매핑 (topic children이 id만 가지므로 join용)
        Map<String, Map<String, Object>> paperById = new LinkedHashMap<>();
        for (Map<String, Object> p : normalizePapers(pickList(raw, "papers", "data", "results", "content"))) {
            paperById.put(String.valueOf(p.get("id")), p);
        }

        List<Map<String, Object>> topics = new ArrayList<>();
        if (((Map<String, Object>) roadmap).get("roots") instanceof List<?> roots) {
            for (Object r : roots) {
                if (!(r instanceof Map)) continue;
                Object nodesObj = ((Map<String, Object>) r).get("intermediate_nodes");
                if (!(nodesObj instanceof List<?> nodes)) continue;
                for (Object n : nodes) {
                    if (!(n instanceof Map)) continue;
                    Map<String, Object> node = (Map<String, Object>) n;

                    List<Map<String, Object>> topicPapers = new ArrayList<>();
                    if (node.get("children") instanceof List<?> children) {
                        for (Object c : children) {
                            if (!(c instanceof Map)) continue;
                            String pid = pick((Map<String, Object>) c, "paper_id", "id", "arxiv_id");
                            Map<String, Object> paper = paperById.get(pid);
                            if (paper != null) {
                                topicPapers.add(paper);
                            }
                        }
                    }

                    Map<String, Object> topic = new LinkedHashMap<>();
                    topic.put("id", pick(node, "node_id", "id"));
                    topic.put("label", pick(node, "label", "name"));
                    topic.put("papers", topicPapers);
                    topics.add(topic);
                }
            }
        }

        // 트리에서 토픽을 못 만들면 카테고리 그룹으로 폴백
        return topics.isEmpty() ? normalizeTopics(raw, keyword) : topics;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTopics(Map<String, Object> raw, String keyword) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        // Python이 이미 topics 구조로 주는 경우: 필드명만 정규화
        if (raw.get("topics") instanceof List<?> topicList) {
            return topicList.stream()
                    .filter(t -> t instanceof Map)
                    .map(t -> {
                        Map<String, Object> topic = (Map<String, Object>) t;
                        String label = pick(topic, "label", "name", "category", "keyword");
                        Map<String, Object> norm = new LinkedHashMap<>();
                        norm.put("id", pickOr(topic, topicId(keyword, label), "id", "topic_id"));
                        norm.put("label", label);
                        norm.put("papers", normalizePapers(topic.get("papers")));
                        return norm;
                    })
                    .collect(Collectors.toList());
        }

        // 평평한 논문 리스트로 주는 경우: 카테고리별로 묶어 topics 생성
        List<Map<String, Object>> papers = normalizePapers(
                pickList(raw, "papers", "data", "results", "content"));
        if (papers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Map<String, Object>>> grouped = papers.stream()
                .collect(Collectors.groupingBy(
                        p -> String.valueOf(p.getOrDefault("privaryCategory", "기타")),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Map<String, Object>> topics = new ArrayList<>();
        grouped.forEach((category, categoryPapers) -> {
            Map<String, Object> topic = new LinkedHashMap<>();
            topic.put("id", topicId(keyword, category));
            topic.put("label", category);
            topic.put("papers", categoryPapers);
            topics.add(topic);
        });
        return topics;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizePapers(Object papersObj) {
        if (!(papersObj instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(p -> p instanceof Map)
                .map(p -> {
                    Map<String, Object> paper = (Map<String, Object>) p;
                    Map<String, Object> norm = new LinkedHashMap<>();
                    norm.put("id", pick(paper, "id", "arxiv_id", "paper_id", "paperId"));
                    norm.put("title", pick(paper, "title"));
                    norm.put("abstracts", pick(paper, "abstracts", "abstract", "summary"));
                    norm.put("author", pick(paper, "author", "submitter", "authors"));
                    norm.put("createdDate", pick(paper, "createdDate", "created_date", "published"));
                    norm.put("privaryCategory", pick(paper, "privaryCategory", "primary_category", "arxiv_primary_category"));
                    return norm;
                })
                .collect(Collectors.toList());
    }

    private String topicId(String keyword, String label) {
        return "topic::" + (keyword != null ? keyword : "") + "::" + label;
    }

    // 여러 후보 키 중 처음으로 값이 있는 것을 반환
    private String pick(Map<String, Object> map, String... keys) {
        return pickOr(map, "", keys);
    }

    private String pickOr(Map<String, Object> map, String fallback, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof List<?> list && !list.isEmpty()) {
                return list.get(0).toString();
            }
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return fallback;
    }

    private Object pickList(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.get(key) instanceof List) {
                return map.get(key);
            }
        }
        return null;
    }

    // POST /api/paper/select — 선택한 논문 데이터를 PostgreSQL에 저장
    @Transactional
    public List<PaperResponseDto> selectAndSave(List<Map<String, Object>> selectedPapers) {
        if (selectedPapers == null || selectedPapers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Paper> toSave = selectedPapers.stream()
                .filter(p -> {
                    String paperId = getStr(p, "arxiv_id", getStr(p, "paper_id"));
                    return !"Unknown".equals(paperId) && !paperRepository.existsById(paperId);
                })
                .map(p -> {
                    Paper paper = new Paper();
                    paper.setPaperId(getStr(p, "arxiv_id", getStr(p, "paper_id")));
                    paper.setTitle(getStr(p, "title"));
                    paper.setAbstracts(getStr(p, "abstract"));
                    paper.setCategories(getStr(p, "categories"));
                    paper.setPrivaryCategory(getStr(p, "primary_category"));
                    paper.setAuthor(getStr(p, "submitter", getStr(p, "author")));
                    paper.setCreatedDate(getStr(p, "created_date", getStr(p, "published")));
                    return paper;
                })
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            paperRepository.saveAll(toSave);
        }

        return selectedPapers.stream()
                .map(p -> paperRepository.findById(getStr(p, "arxiv_id", getStr(p, "paper_id")))
                        .map(PaperResponseDto::new)
                        .orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    // null이면 "Unknown" 반환
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(" "));
        }
        return (val != null && !val.toString().isBlank()) ? val.toString() : "Unknown";
    }

    private String getStr(Map<String, Object> map, String key, String fallback) {
        String value = getStr(map, key);
        return "Unknown".equals(value) ? fallback : value;
    }

    // GET /api/paper/{id}
    @Transactional(readOnly = true)
    public PaperResponseDto getById(String paperId) {
        return paperRepository.findById(paperId)
                .map(PaperResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("논문을 찾을 수 없습니다: " + paperId));
    }

    @Transactional(readOnly = true)
    public List<PaperResponseDto> getPapers(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        if (keyword != null && !keyword.isBlank()) {
            return paperRepository.searchByKeyword(keyword, pageable).stream()
                    .map(PaperResponseDto::new)
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            return paperRepository.findByPrivaryCategory(category, pageable).stream()
                    .map(PaperResponseDto::new)
                    .collect(Collectors.toList());
        }
        return paperRepository.findAll(pageable).stream()
                .map(PaperResponseDto::new)
                .collect(Collectors.toList());
    }
}
