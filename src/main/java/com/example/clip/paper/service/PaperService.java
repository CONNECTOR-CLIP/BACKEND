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

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

// 논문 검색/저장/조회 비즈니스 로직 — Python 검색 API 연동 및 RDS 저장 처리
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperRepository paperRepository;
    private final WebClient webClient;

    @Value("${python.api.base-url}")
    private String pythonBaseUrl;

    // 키워드/카테고리/페이지 조건을 Python 검색 API에 전달하고 결과를 그대로 반환
    public Map<String, Object> search(SearchRequestDto requestDto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("keyword", requestDto.getKeyword() != null ? requestDto.getKeyword() : "");
        payload.put("page", requestDto.getPage());
        payload.put("size", requestDto.getSize());
        // category가 있을 때만 payload에 포함
        if (requestDto.getCategory() != null && !requestDto.getCategory().isBlank()) {
            payload.put("category", requestDto.getCategory());
        }

        Map<String, Object> result = webClient.post()
                .uri(pythonBaseUrl + "/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return result != null ? result : Collections.emptyMap();
    }

    // 이미 저장된 논문은 건너뛰고 신규 논문만 RDS에 저장
    @Transactional
    public List<PaperResponseDto> selectAndSave(List<Map<String, Object>> selectedPapers) {
        if (selectedPapers == null || selectedPapers.isEmpty()) {
            return Collections.emptyList();
        }

        // arxiv_id 또는 paper_id를 기본 키로 사용, "Unknown"이거나 이미 존재하면 제외
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
                    // submitter 또는 author 필드 우선순위 적용
                    paper.setAuthor(getStr(p, "submitter", getStr(p, "author")));
                    paper.setCreatedDate(getStr(p, "created_date", getStr(p, "published")));
                    return paper;
                })
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            paperRepository.saveAll(toSave);
        }

        // 저장 후 DB에서 다시 조회해 DTO로 변환 (저장 실패한 논문은 제외)
        return selectedPapers.stream()
                .map(p -> paperRepository.findById(getStr(p, "arxiv_id", getStr(p, "paper_id")))
                        .map(PaperResponseDto::new)
                        .orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    // Map에서 문자열 값 추출, 없거나 비어있으면 "Unknown" 반환
    // List 타입인 경우 공백으로 이어붙임 (authors 필드 대응)
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(" "));
        }
        return (val != null && !val.toString().isBlank()) ? val.toString() : "Unknown";
    }

    // key 값이 "Unknown"이면 fallback 값 사용
    private String getStr(Map<String, Object> map, String key, String fallback) {
        String value = getStr(map, key);
        return "Unknown".equals(value) ? fallback : value;
    }

    // paperId로 논문 단건 조회, 없으면 예외 발생
    @Transactional(readOnly = true)
    public PaperResponseDto getById(String paperId) {
        return paperRepository.findById(paperId)
                .map(PaperResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("논문을 찾을 수 없습니다: " + paperId));
    }

    // 키워드 → 카테고리 → 전체 순서로 조건 분기해 페이징 조회
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
