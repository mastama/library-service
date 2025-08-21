package com.yolifay.libraryservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="library_items")
@Getter @Setter
public class LibraryItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name="author_id")
    private Long authorId;
    private Instant createdAt;
    private Instant updatedAt;
}
