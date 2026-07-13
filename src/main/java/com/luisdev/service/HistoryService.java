package com.luisdev.service;

import com.luisdev.domain.entity.History;
import com.luisdev.domain.entity.User;
import com.luisdev.repository.HistoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {
  private final HistoryRepository historyRepository;

  public HistoryService(HistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  @Async
  public void recordHistory(User user, String actionType, String itemType, String itemName) {
    History history = History.builder()
        .user(user)
        .actionType(actionType)
        .itemType(itemType)
        .itemName(itemName)
        .build();
    historyRepository.save(history);
  }
}
