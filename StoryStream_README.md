# StoryStream — Audiobook Discovery Platform

Spring Boot REST API + PostgreSQL + React (Vite) UI for browsing an audiobook catalog,
saving bookmarks, tracking listening progress, and getting recommendations.

## Architecture
```
React (Vite, :5173) ──HTTP/JSON──> Spring Boot API (:8080) ──JPA──> PostgreSQL 16
                                    controllers → services → repositories
```
- `web/` — REST controllers, request/response DTOs, global JSON error handler
- `service/` — catalog search, recommendations, bookmarks, listening history
- `repository/` — Spring Data JPA queries (paginated search, similar books)
- `config/` — CORS, demo data seeding, PostgreSQL trigram search indexes

## Requirements
Java 17+, Maven 3.9+, Docker, Node.js 20+.

## Run it
```bash
# 1. Database
docker compose up -d db

# 2. API (new terminal)
cd backend
mvn spring-boot:run            # http://localhost:8080/api/audiobooks

# 3. UI (new terminal)
cd frontend
npm install
npm run dev                    # http://localhost:5173
```
Or run the database and API together in Docker: `docker compose --profile full up --build`.

## Tests and coverage
```bash
cd backend
mvn verify                     # runs JUnit tests (H2 in PostgreSQL mode)
open target/site/jacoco/index.html   # coverage report
```

## Search performance benchmark
`LIKE '%term%'` can't use a normal B-tree index, so the API creates PostgreSQL
**trigram GIN indexes** on `lower(title)` and `lower(author)` at startup.
Measure the effect on 200,000 synthetic rows (uses its own table, app data untouched):
```bash
docker compose exec -T db psql -U storystream -d storystream < scripts/benchmark_search.sql
```
It prints the query plan before/after (Seq Scan → Bitmap Index Scan) and median
latency over 30 runs.

Results (200,000 rows, median of 30 runs): 51 ms → under 1 ms (~99% reduction).
Trade-offs: trigram indexes need at least 3 characters to help, and they make
writes slightly slower and take extra disk space.

## API
| Method | Path | Notes |
|---|---|---|
| GET | `/api/audiobooks?query=&genre=&page=0&size=12&sort=title&direction=asc` | paginated; `sort` ∈ title, author, rating, durationMinutes |
| GET | `/api/audiobooks/{id}` | 404 JSON error if missing |
| POST | `/api/audiobooks` | validated body |
| GET | `/api/audiobooks/{id}/similar?limit=5` | same genre or author, best rated |
| PUT | `/api/users/{userId}/bookmarks/{bookId}` | `{"positionSeconds":420}`, one per user+book |
| GET / DELETE | `/api/users/{userId}/bookmarks[/{bookId}]` | |
| POST | `/api/users/{userId}/history/{bookId}` | `{"secondsListened":900}`, adds to running total |
| GET | `/api/users/{userId}/history` | includes percent complete |
| GET | `/api/users/{userId}/recommendations?limit=5` | ranked by the genres you listen to most |
| GET | `/actuator/health` | health check |

```bash
curl "http://localhost:8080/api/audiobooks?query=weir&sort=rating&direction=desc"
curl -X PUT http://localhost:8080/api/users/demo-user/bookmarks/1 -H 'Content-Type: application/json' -d '{"positionSeconds":420}'
curl -X POST http://localhost:8080/api/users/demo-user/history/1 -H 'Content-Type: application/json' -d '{"secondsListened":900}'
curl http://localhost:8080/api/users/demo-user/recommendations
```
`userId` is a plain identifier for the demo, not authentication.
