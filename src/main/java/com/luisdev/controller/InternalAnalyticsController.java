package com.luisdev.controller;

import com.luisdev.dto.InternalHistoryResponse;
import com.luisdev.repository.HistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service-to-service API consumed by kubo-analytics (FastAPI).
 *
 * Not a browser endpoint. The caller authenticates with X-Internal-Token — see
 * {@link com.luisdev.security.InternalTokenFilter} — never with the user's jwt
 * cookie, and the reverse proxy must NOT expose /api/internal/** publicly.
 *
 * The feed is served over HTTP rather than letting the analytics service read
 * Postgres directly: Hibernate owns the schema here
 * ({@code ddl-auto=update}), so a JSON contract survives a column rename and a
 * table name does not.
 */
@RestController
@RequestMapping("/api/internal/analytics")
public class InternalAnalyticsController {

  /** Same window bounds the analytics service enforces on its own edge. */
  private static final int MIN_DAYS = 1;
  private static final int MAX_DAYS = 365;
  private static final int DEFAULT_DAYS = 30;

  private final HistoryRepository historyRepository;

  public InternalAnalyticsController(HistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  /**
   * Raw history rows from the last {@code days} days, oldest first.
   *
   * An omitted {@code email} means every user. Gating that on ROLE_ADMIN is
   * the analytics service's job — this endpoint trusts the internal token and
   * nothing else, because there is no user session here to check a role
   * against.
   *
   * Returns a plain array, not a Page: the consumer aggregates the whole
   * window in pandas, so paging would only force it to loop.
   */
  @GetMapping("/history")
  public ResponseEntity<List<InternalHistoryResponse>> history(
      @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days,
      @RequestParam(required = false) String email) {

    int window = Math.min(Math.max(days, MIN_DAYS), MAX_DAYS);

    // Start of day, inclusive of today: mirrors the pandas window in
    // activity.py, which spans `days` calendar days ending today. Using
    // now().minusDays() instead would drop this morning's events from the
    // oldest day and make the first bar of every chart wrong.
    LocalDateTime since = LocalDateTime.now()
        .toLocalDate()
        .minusDays(window - 1L)
        .atStartOfDay();

    return ResponseEntity.ok(historyRepository.findForAnalytics(since, email));
  }
}
