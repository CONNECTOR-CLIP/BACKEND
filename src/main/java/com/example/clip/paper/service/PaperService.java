package com.example.clip.paper.service;

import com.example.clip.paper.domain.Paper;
import com.example.clip.paper.dto.PaperResponseDto;
import com.example.clip.paper.dto.SearchRequestDto;
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
    public List<Map<String, Object>> search(SearchRequestDto requestDto) {
        Map<String, Object> result = webClient.post()
                .uri(pythonBaseUrl + "/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("keyword", requestDto.getKeyword(),
                                  "page",    requestDto.getPage(),
                                  "size",    requestDto.getSize()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (List<Map<String, Object>>) result.get("papers");
    }

    // POST /api/paper/select — 선택한 논문 데이터를 PostgreSQL에 저장
    @Transactional
    public List<PaperResponseDto> selectAndSave(List<Map<String, Object>> selectedPapers) {
        List<Paper> toSave = selectedPapers.stream()
                .filter(p -> !paperRepository.existsById((String) p.get("arxiv_id")))
                .map(p -> {
                    Paper paper = new Paper();
                    paper.setPaperId(getStr(p, "arxiv_id"));
                    paper.setTitle(getStr(p, "title"));
                    paper.setAbstracts(getStr(p, "abstract"));
                    paper.setCategories(getStr(p, "categories"));
                    paper.setPrivaryCategory(getStr(p, "primary_category"));
                    paper.setAuthor(getStr(p, "submitter"));   // null이면 "Unknown"
                    paper.setCreatedDate(getStr(p, "created_date"));
                    return paper;
                })
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            paperRepository.saveAll(toSave);
        }

        return selectedPapers.stream()
                .map(p -> paperRepository.findById(getStr(p, "arxiv_id"))
                        .map(PaperResponseDto::new)
                        .orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    // null이면 "Unknown" 반환
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString() : "Unknown";
    }

    // GET /api/paper/{id}
    @Transactional(readOnly = true)
    public PaperResponseDto getById(String paperId) {
        return paperRepository.findById(paperId)
                .map(PaperResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("논문을 찾을 수 없습니다: " + paperId));
    }
}
