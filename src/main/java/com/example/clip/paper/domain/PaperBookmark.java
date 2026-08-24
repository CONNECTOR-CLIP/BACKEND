package com.example.clip.paper.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "paper_bookmark",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "paper_id"}))
public class PaperBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "paper_id", nullable = false)
    private String paperId;

    @Column(name = "title")
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "category")
    private String category;

    public PaperBookmark(String userId, String paperId, String title, String author, String category) {
        this.userId = userId;
        this.paperId = paperId;
        this.title = title;
        this.author = author;
        this.category = category;
    }
}
