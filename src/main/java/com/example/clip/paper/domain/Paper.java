package com.example.clip.paper.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "\"Paper\"")
public class Paper {

    @Id
    @Column(name = "paper_id")
    private String paperId;

    @Column(name = "author")
    private String author;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "source")
    private String source;

    @Column(name = "\"abstract\"", columnDefinition = "TEXT")
    private String abstracts;

    @Column(name = "papertext", columnDefinition = "TEXT")
    private String papertext;

    @Column(name = "privary_category")
    private String privaryCategory;

    @Column(name = "categories")
    private String categories;

    @Column(name = "oai_identifier")
    private String oaiIdentifier;
}
