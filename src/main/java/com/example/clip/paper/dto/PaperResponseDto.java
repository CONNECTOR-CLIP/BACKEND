package com.example.clip.paper.dto;

import com.example.clip.paper.domain.Paper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 논문 응답 DTO — Paper 엔티티에서 클라이언트에 노출할 필드만 추출
@Getter
@Setter
@NoArgsConstructor
public class PaperResponseDto {
    private String paperId;
    private String title;
    private String author;
    private String abstracts;
    private String categories;
    private String privaryCategory;
    private String source;
    private String createdDate;

    public PaperResponseDto(Paper paper) {
        this.paperId = paper.getPaperId();
        this.title = paper.getTitle();
        this.author = paper.getAuthor();
        this.abstracts = paper.getAbstracts();
        this.categories = paper.getCategories();
        this.privaryCategory = paper.getPrivaryCategory();
        this.source = paper.getSource();
        this.createdDate = paper.getCreatedDate();
    }
}
