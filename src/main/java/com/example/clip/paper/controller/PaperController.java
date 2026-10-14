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

// 논문 검색/조회/저장 및 로드맵 관련 API 처리
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    // POST /api/search — Python 검색 API를 프록시하여 논문 검색 결과 반환
    @PostMapping("/api/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequestDto requestDto) {
        try {
            Map<String, Object> result = paperService.search(requestDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("논문 검색 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/roadmap?keyword= — 키워드로 논문 검색 후 로드맵 데이터로 반환, keyword 필수
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
            return ResponseEntity.ok(paperService.search(requestDto));
        } catch (Exception e) {
            log.error("로드맵 생성 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/roadmap/node/{id} — 노드 id가 논문이면 상세 정보, 카테고리이면 label 정보 반환
    @GetMapping("/api/roadmap/node/{id}")
    public ResponseEntity<Map<String, Object>> getRoadmapNode(@PathVariable String id) {
        try {
            PaperResponseDto paper = paperService.getById(id);
            return ResponseEntity.ok(Map.of(
                    "id", paper.getPaperId(),
                    "type", "paper",
                    "paper", paper
            ));
        } catch (IllegalArgumentException e) {
            // id가 논문 ID가 아닌 카테고리 노드일 경우
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "type", "category",
                    "label", id
            ));
        }
    }

    // POST /api/paper/select — 사용자가 선택한 논문 목록을 PostgreSQL에 저장
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

    // GET /api/paper — 키워드/카테고리 기반 논문 목록 페이징 조회
    @GetMapping("/api/paper")
    public ResponseEntity<List<PaperResponseDto>> getPapers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paperService.getPapers(keyword, category, page, size));
    }

    // GET /api/paper/{id} — 논문 단건 상세 조회, 없으면 404 반환
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
