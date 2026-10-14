package com.example.clip.gap.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

// 논문 초안 생성 요청 DTO — 갭 분석에서 선택한 제안(proposal) 데이터를 담아 Python으로 전달
@Getter
@Setter
public class DraftRequestDto {
    private Map<String, Object> proposal;
}
