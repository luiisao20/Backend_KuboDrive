package com.luisdev.repository;

import com.luisdev.domain.entity.History;
import com.luisdev.dto.InternalHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface HistoryRepository extends JpaRepository<History, UUID> {

  @Query(value = """
      SELECT h FROM History h WHERE h.user.id = :userId
      AND (cast(:actionType as text) IS NULL OR h.actionType = cast(:actionType as text))
      AND (cast(:itemName as text) IS NULL OR LOWER(h.itemName) LIKE LOWER(CONCAT('%', cast(:itemName as text), '%')))
      AND (cast(cast(:startDate as text) as timestamp) IS NULL OR h.timestamp >= :startDate)
      AND (cast(cast(:endDate as text) as timestamp) IS NULL OR h.timestamp <= :endDate)
      order by h.timestamp
    """)
  Page<History> findHistoryWithFilters(
      @Param("userId") UUID userId,
      @Param("actionType") String actionType,
      @Param("itemName") String itemName,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  /**
   * The analytics feed: every event newer than {@code since}, oldest first.
   *
   * A constructor expression rather than the entity, so {@code h.user.email}
   * resolves to a join inside this one query. Returning History would hit the
   * LAZY user proxy once per row — the classic N+1 — or fail outright once the
   * transaction closed.
   *
   * {@code email} is optional: null means every user. The
   * {@code cast(... as text)} dance is the same one findHistoryWithFilters
   * needs — PostgreSQL cannot infer the type of a bare null bind parameter and
   * rejects the statement without it.
   */
  @Query(value = """
      SELECT new com.luisdev.dto.InternalHistoryResponse(
          h.actionType, h.itemType, h.itemName, h.user.email, h.timestamp)
      FROM History h
      WHERE h.timestamp >= :since
      AND (cast(:email as text) IS NULL OR LOWER(h.user.email) = LOWER(cast(:email as text)))
      order by h.timestamp
    """)
  List<InternalHistoryResponse> findForAnalytics(
      @Param("since") LocalDateTime since,
      @Param("email") String email);
}
