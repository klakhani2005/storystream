package com.portfolio.storystream.web;

import com.portfolio.storystream.service.CatalogService;
import com.portfolio.storystream.web.Dtos.AudiobookResponse;
import com.portfolio.storystream.web.Dtos.CreateAudiobookRequest;
import com.portfolio.storystream.web.Dtos.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audiobooks")
@Validated
public class CatalogController {

    private static final Set<String> SORTABLE = Set.of("title", "author", "rating", "durationMinutes");

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public PageResponse<AudiobookResponse> browse(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        if (!SORTABLE.contains(sort)) {
            throw new IllegalArgumentException("sort must be one of " + SORTABLE);
        }
        Sort order = "desc".equalsIgnoreCase(direction) ? Sort.by(sort).descending() : Sort.by(sort).ascending();
        var pageable = PageRequest.of(page, size, order.and(Sort.by("id")));
        return PageResponse.from(catalog.browse(query, genre, pageable), AudiobookResponse::from);
    }

    @GetMapping("/{id}")
    public AudiobookResponse get(@PathVariable Long id) {
        return AudiobookResponse.from(catalog.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AudiobookResponse create(@Valid @RequestBody CreateAudiobookRequest req) {
        return AudiobookResponse.from(catalog.create(req.title(), req.author(), req.genre(),
                req.durationMinutes(), req.description(), req.rating()));
    }

    @GetMapping("/{id}/similar")
    public List<AudiobookResponse> similar(@PathVariable Long id,
                                           @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return catalog.similarTo(id, limit).stream().map(AudiobookResponse::from).toList();
    }
}
