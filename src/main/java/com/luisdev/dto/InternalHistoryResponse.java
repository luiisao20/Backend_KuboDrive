package com.luisdev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One domain event, flattened for kubo-analytics.
 *
 * The field names ARE the contract with the Python service: they are read by
 * name in {@code app/analytics/activity.py} (REQUIRED_COLUMNS, plus
 * timestamp). Renaming one here does not break the build — it silently empties
 * a column in the dashboard. Keep both sides in lockstep.
 *
 * Flat on purpose: {@code History.user} is LAZY, so serializing the entity
 * would either blow up outside the transaction or drag the whole User graph
 * (password hash included) across the wire. Only the email is needed.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalHistoryResponse {
  private String actionType;
  private String itemType;
  private String itemName;
  private String userEmail;
  private LocalDateTime timestamp;
}
