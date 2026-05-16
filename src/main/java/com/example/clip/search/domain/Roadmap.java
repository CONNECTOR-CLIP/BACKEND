package com.example.clip.search.domain;

import com.example.clip.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "\"Roadmap\"")
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmap_id")
    private Long roadmapId;

    @Column(name = "roadmap_title", nullable = false, length = 100)
    private String roadmapTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Roadmap(String roadmapTitle, User user) {
        this.roadmapTitle = roadmapTitle;
        this.user = user;
    }
}
