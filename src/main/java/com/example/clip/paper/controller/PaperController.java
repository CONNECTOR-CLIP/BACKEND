package com.example.clip.paper.controller;

import com.example.clip.paper.dto.PaperResponseDto;
import com.example.clip.paper.dto.SearchRequestDto;
import com.example.clip.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    // POST /api/search — Python 검색 결과를 { topics: [...] } 구조로 정규화해 반환
    @PostMapping("/api/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequestDto requestDto) {
        try {
            Map<String, Object> result = paperService.searchTopics(requestDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("논문 검색 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "검색에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    // GET /api/roadmap — 프론트 로드맵 페이지 호환 API
    @GetMapping("/api/roadmap")
    public ResponseEntity<Map<String, Object>> getRoadmap(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (keyword == null || keyword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "keyword 값이 필요합니다."));
            }
            SearchRequestDto requestDto = new SearchRequestDto();
            requestDto.setKeyword(keyword);
            requestDto.setPage(1);
            requestDto.setSize(size);
            return ResponseEntity.ok(paperService.roadmap(requestDto));
        } catch (Exception e) {
            log.error("로드맵 생성 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "로드맵을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    // GET /api/roadmap/node/{id} — 로드맵 노드 상세. id는 URL 인코딩됨(`::` 포함)
    @GetMapping("/api/roadmap/node/{id}")
    public ResponseEntity<Map<String, Object>> getRoadmapNode(@PathVariable String id) {
        try {
            // 주제 노드: 로드맵 조회(3-1) 시 캐시된 상세({ label, summary, keyTerms, papers }) 반환
            Map<String, Object> node = paperService.roadmapNode(id);
            if (node != null) {
                return ResponseEntity.ok(node);
            }
            // 캐시에 없으면 논문 id로 간주 → 단일 논문 상세 (하위 호환)
            PaperResponseDto paper = paperService.getById(id);
            return ResponseEntity.ok(Map.of(
                    "id", paper.getPaperId(),
                    "type", "paper",
                    "paper", paper
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "노드를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("노드 상세 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "노드 상세 조회에 실패했습니다."));
        }
    }

    // POST /api/paper/select — 선택한 논문 PostgreSQL 저장
    @PostMapping("/api/paper/select")
    public ResponseEntity<List<PaperResponseDto>> selectPapers(
            @RequestBody Map<String, List<Map<String, Object>>> body) {
        try {
            List<Map<String, Object>> selectedPapers = body.get("papers");
            List<PaperResponseDto> result = paperService.selectAndSave(selectedPapers);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("논문 선택 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/paper/{id} — 논문 상세
    @GetMapping("/api/paper")
    public ResponseEntity<List<PaperResponseDto>> getPapers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paperService.getPapers(keyword, category, page, size));
    }

    // GET /api/paper/{id} — 논문 상세
    @GetMapping("/api/paper/{id}")
    public ResponseEntity<PaperResponseDto> getPaper(@PathVariable String id) {
        try {
            return ResponseEntity.ok(paperService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("논문 상세 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
