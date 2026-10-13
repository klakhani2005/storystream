package com.portfolio.storystream.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audiobooks", indexes = {
        // B-tree indexes for exact genre filters and rating sorts.
        // Substring search uses trigram indexes created in SearchIndexInitializer.
        @Index(name = "idx_audiobook_genre_rating", columnList = "genre, rating"),
        @Index(name = "idx_audiobook_author", columnList = "author")
})
public class Audiobook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 80)
    private String genre;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(length = 700)
    private String description;

    @Column(nullable = false)
    private double rating;

    protected Audiobook() {
        // for JPA
    }

    public Audiobook(String title, String author, String genre, int durationMinutes,
                     String description, double rating) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
}
