package com.example.clip.paper.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gap_bookmark",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gap_id"}))
public class GapBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "gap_id", nullable = false)
    private String gapId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public GapBookmark(String userId, String gapId, String title, String description) {
        this.userId = userId;
        this.gapId = gapId;
        this.title = title;
        this.description = description;
    }
}
