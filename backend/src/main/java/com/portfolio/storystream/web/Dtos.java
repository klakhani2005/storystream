package com.portfolio.storystream.web;

import com.portfolio.storystream.model.Audiobook;
import com.portfolio.storystream.model.Bookmark;
import com.portfolio.storystream.model.ListeningHistory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Request/response shapes, kept separate from JPA entities. */
public final class Dtos {

    private Dtos() {
    }

    static final String USER_ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    public record AudiobookResponse(Long id, String title, String author, String genre,
                                    int durationMinutes, String description, double rating) {
        public static AudiobookResponse from(Audiobook a) {
            return new AudiobookResponse(a.getId(), a.getTitle(), a.getAuthor(), a.getGenre(),
                    a.getDurationMinutes(), a.getDescription(), a.getRating());
        }
    }

    public record CreateAudiobookRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 120) String author,
            @NotBlank @Size(max = 80) String genre,
            @Min(1) @Max(10_000) int durationMinutes,
            @Size(max = 700) String description,
            @DecimalMin("0.0") @DecimalMax("5.0") double rating) {
    }

    public record BookmarkResponse(Long audiobookId, String title, int positionSeconds, Instant updatedAt) {
        public static BookmarkResponse from(Bookmark b) {
            return new BookmarkResponse(b.getAudiobook().getId(), b.getAudiobook().getTitle(),
                    b.getPositionSeconds(), b.getUpdatedAt());
        }
    }

    public record HistoryResponse(Long audiobookId, String title, String genre, int secondsListened,
                                  int percentComplete, Instant updatedAt) {
        public static HistoryResponse from(ListeningHistory h) {
            int total = h.getAudiobook().getDurationMinutes() * 60;
            int pct = total == 0 ? 0 : Math.min(100, (int) Math.round(100.0 * h.getSecondsListened() / total));
            return new HistoryResponse(h.getAudiobook().getId(), h.getAudiobook().getTitle(),
                    h.getAudiobook().getGenre(), h.getSecondsListened(), pct, h.getUpdatedAt());
        }
    }

    /** Stable JSON shape for paginated results (instead of serializing Spring's PageImpl). */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
