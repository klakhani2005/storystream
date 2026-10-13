package com.portfolio.storystream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.storystream.model.Audiobook;
import com.portfolio.storystream.repository.AudiobookRepository;
import com.portfolio.storystream.repository.BookmarkRepository;
import com.portfolio.storystream.repository.ListeningHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AudiobookRepository books;
    @Autowired BookmarkRepository bookmarks;
    @Autowired ListeningHistoryRepository history;

    Long martianId;
    Long duneId;
    Long educatedId;

    @BeforeEach
    void seed() {
        bookmarks.deleteAll();
        history.deleteAll();
        books.deleteAll();
        martianId = books.save(new Audiobook("The Martian", "Andy Weir", "Science Fiction", 660, "Mars", 4.8)).getId();
        books.save(new Audiobook("Project Hail Mary", "Andy Weir", "Science Fiction", 960, "Space", 4.9));
        duneId = books.save(new Audiobook("Dune", "Frank Herbert", "Science Fiction", 1260, "Desert", 4.7)).getId();
        educatedId = books.save(new Audiobook("Educated", "Tara Westover", "Memoir", 720, "Memoir", 4.6)).getId();
        books.save(new Audiobook("Born a Crime", "Trevor Noah", "Memoir", 530, "Memoir", 4.8));
    }

    // ---- catalog ----------------------------------------------------------

    @Test
    void browseReturnsPaginatedResults() throws Exception {
        mvc.perform(get("/api/audiobooks").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void searchByAuthorSortedByRating() throws Exception {
        mvc.perform(get("/api/audiobooks").param("query", "WEIR").param("sort", "rating").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Project Hail Mary"))
                .andExpect(jsonPath("$.content[1].title").value("The Martian"));
    }

    @Test
    void rejectsBadPaginationAndSortParams() throws Exception {
        mvc.perform(get("/api/audiobooks").param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/audiobooks").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/audiobooks").param("sort", "password")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownBookIs404WithJsonError() throws Exception {
        mvc.perform(get("/api/audiobooks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    void createValidatesInput() throws Exception {
        mvc.perform(post("/api/audiobooks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"A\",\"genre\":\"G\",\"durationMinutes\":0,\"rating\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        mvc.perform(post("/api/audiobooks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"author\":\"A\",\"genre\":\"G\",\"durationMinutes\":60,\"rating\":4.2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/audiobooks").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similarExcludesTheBookItself() throws Exception {
        mvc.perform(get("/api/audiobooks/{id}/similar", martianId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Project Hail Mary"));
    }

    // ---- bookmarks --------------------------------------------------------

    @Test
    void savingABookmarkTwiceMovesItInsteadOfDuplicating() throws Exception {
        String path = "/api/users/karim/bookmarks/" + martianId;
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"positionSeconds\":120}"))
                .andExpect(status().isOk());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"positionSeconds\":900}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionSeconds").value(900));

        mvc.perform(get("/api/users/karim/bookmarks"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("The Martian"));

        mvc.perform(delete(path)).andExpect(status().isNoContent());
        mvc.perform(delete(path)).andExpect(status().isNotFound());
    }

    @Test
    void bookmarkPastEndOfBookIsRejected() throws Exception {
        mvc.perform(put("/api/users/karim/bookmarks/" + martianId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionSeconds\":99999999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidUserIdIsRejected() throws Exception {
        mvc.perform(get("/api/users/{u}/bookmarks", "bad user!"))
                .andExpect(status().isBadRequest());
    }

    // ---- history + recommendations ----------------------------------------

    @Test
    void listeningSessionsAccumulate() throws Exception {
        String path = "/api/users/karim/history/" + martianId;
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"secondsListened\":600}"))
                .andExpect(status().isCreated());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"secondsListened\":900}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secondsListened").value(1500))
                .andExpect(jsonPath("$.percentComplete").value(4));
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"secondsListened\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recommendationsFollowListeningAndSkipStartedBooks() throws Exception {
        mvc.perform(post("/api/users/karim/history/" + educatedId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"secondsListened\":3000}"));
        mvc.perform(post("/api/users/karim/history/" + duneId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"secondsListened\":60}"));

        // Mostly memoir listening -> memoir first; Educated and Dune are already started.
        mvc.perform(get("/api/users/karim/recommendations").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Born a Crime"))
                .andExpect(jsonPath("$[*].title", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Dune"))));
    }

    @Test
    void newUserGetsTopRatedBooks() throws Exception {
        mvc.perform(get("/api/users/newbie/recommendations").param("limit", "1"))
                .andExpect(jsonPath("$[0].title").value("Project Hail Mary"));
    }

    @Test
    void corsAllowsTheFrontendOrigin() throws Exception {
        mvc.perform(options("/api/audiobooks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
