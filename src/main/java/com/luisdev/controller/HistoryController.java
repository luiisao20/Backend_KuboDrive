package com.luisdev.controller;

import com.luisdev.domain.entity.History;
import com.luisdev.dto.HistoryResponse;
import com.luisdev.repository.HistoryRepository;
import com.luisdev.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

  private final HistoryRepository historyRepository;
  private final UserService userService;

  public HistoryController(HistoryRepository historyRepository, UserService userService) {
    this.historyRepository = historyRepository;
    this.userService = userService;
  }

  private UUID getCurrentUserId() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return userService.findByEmail(email).getId();
  }

  @GetMapping
  public ResponseEntity<Page<HistoryResponse>> getHistory(
      @RequestParam(required = false) String actionType,
      @RequestParam(required = false) String itemName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
      @PageableDefault(size = 10) Pageable pageable) {

    Page<History> historyPage = historyRepository.findHistoryWithFilters(
        getCurrentUserId(), actionType, itemName, startDate, endDate, pageable);

    Page<HistoryResponse> responsePage = historyPage.map(h -> HistoryResponse.builder()
        .id(h.getId())
        .actionType(h.getActionType())
        .itemType(h.getItemType())
        .itemName(h.getItemName())
        .timestamp(h.getTimestamp())
        .build());

    return ResponseEntity.ok(responsePage);
  }
}
