package com.luisdev.repository;

import com.luisdev.domain.entity.History;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
}
