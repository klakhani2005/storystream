package com.portfolio.storystream.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.storystream.model.Audiobook;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class AudiobookRepositoryTest {

    @Autowired
    AudiobookRepository repo;

    Audiobook martian;

    @BeforeEach
    void seed() {
        martian = repo.save(new Audiobook("The Martian", "Andy Weir", "Science Fiction", 660, "Mars", 4.8));
        repo.save(new Audiobook("Project Hail Mary", "Andy Weir", "Science Fiction", 960, "Space", 4.9));
        repo.save(new Audiobook("Dune", "Frank Herbert", "Science Fiction", 1260, "Desert", 4.7));
        repo.save(new Audiobook("Educated", "Tara Westover", "Memoir", 720, "Memoir", 4.6));
        repo.save(new Audiobook("100% Focus", "Some Author", "Productivity", 300, "Literal percent", 4.0));
    }

    @Test
    void searchMatchesTitleOrAuthorCaseInsensitively() {
        var byTitle = repo.search("%martian%", PageRequest.of(0, 10));
        var byAuthor = repo.search("%weir%", PageRequest.of(0, 10));
        assertThat(byTitle.getContent()).extracting(Audiobook::getTitle).containsExactly("The Martian");
        assertThat(byAuthor.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchIsPaginated() {
        var first = repo.search("%e%", PageRequest.of(0, 2, Sort.by("title")));
        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isGreaterThan(2);
        var second = repo.search("%e%", PageRequest.of(1, 2, Sort.by("title")));
        assertThat(second.getContent()).doesNotContainAnyElementsOf(first.getContent());
    }

    @Test
    void escapedPercentIsMatchedLiterally() {
        // "%100\%%" means: contains the literal text "100%"
        assertThat(repo.search("%100\\%%", PageRequest.of(0, 10)).getContent())
                .extracting(Audiobook::getTitle).containsExactly("100% Focus");
    }

    @Test
    void similarReturnsSameGenreOrAuthorBestRatedFirstExcludingSource() {
        var similar = repo.findSimilar("science fiction", "Andy Weir", Set.of(martian.getId()), PageRequest.of(0, 5));
        assertThat(similar).extracting(Audiobook::getTitle).containsExactly("Project Hail Mary", "Dune");
    }

    @Test
    void genreFilterIgnoresCase() {
        assertThat(repo.findByGenreIgnoreCase("MEMOIR", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }
}
