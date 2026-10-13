package com.portfolio.storystream.service;

import com.portfolio.storystream.model.Audiobook;
import com.portfolio.storystream.repository.AudiobookRepository;
import com.portfolio.storystream.repository.ListeningHistoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final AudiobookRepository books;
    private final ListeningHistoryRepository history;

    public CatalogService(AudiobookRepository books, ListeningHistoryRepository history) {
        this.books = books;
        this.history = history;
    }

    /** Paginated catalog: search by title/author, filter by genre, or list everything. */
    public Page<Audiobook> browse(String query, String genre, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return books.search(likePattern(query), pageable);
        }
        if (genre != null && !genre.isBlank()) {
            return books.findByGenreIgnoreCase(genre.trim(), pageable);
        }
        return books.findAll(pageable);
    }

    public Audiobook get(Long id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Audiobook not found: " + id));
    }

    @Transactional
    public Audiobook create(String title, String author, String genre, int durationMinutes,
                            String description, double rating) {
        return books.save(new Audiobook(title.trim(), author.trim(), genre.trim(), durationMinutes,
                description, rating));
    }

    /** "More like this": same genre or author, highest rated first. */
    public List<Audiobook> similarTo(Long id, int limit) {
        Audiobook source = get(id);
        return books.findSimilar(source.getGenre(), source.getAuthor(), Set.of(id), PageRequest.of(0, limit));
    }

    /**
     * Personalized picks: weight genres by how long the user listened to them, then
     * return top-rated books from those genres that the user hasn't started yet.
     */
    public List<Audiobook> recommendedFor(String userId, int limit) {
        var entries = history.findByUserIdOrderByUpdatedAtDesc(userId);
        if (entries.isEmpty()) {
            return books.findAll(PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("rating").descending()))
                    .getContent();
        }
        Map<String, Integer> secondsByGenre = new LinkedHashMap<>();
        for (var e : entries) {
            secondsByGenre.merge(e.getAudiobook().getGenre().toLowerCase(Locale.ROOT), e.getSecondsListened(), Integer::sum);
        }
        List<String> topGenres = secondsByGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        Set<Long> alreadyStarted = entries.stream().map(e -> e.getAudiobook().getId()).collect(Collectors.toSet());
        // Candidates come back best-rated first; re-rank so the user's favourite genre leads,
        // keeping rating order within each genre.
        List<Audiobook> candidates = new ArrayList<>(
                books.findTopInGenres(topGenres, alreadyStarted, PageRequest.of(0, limit * 5)));
        candidates.sort(java.util.Comparator.comparingInt(
                b -> topGenres.indexOf(b.getGenre().toLowerCase(Locale.ROOT))));
        return candidates.subList(0, Math.min(limit, candidates.size()));
    }

    /** Lower-case, escape LIKE wildcards in user input, and wrap in % for substring match. */
    static String likePattern(String query) {
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
