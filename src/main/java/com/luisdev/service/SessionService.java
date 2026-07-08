package com.luisdev.service;

import com.luisdev.domain.entity.Session;
import com.luisdev.domain.entity.User;
import com.luisdev.repository.SessionRepository;
import com.luisdev.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public void createSession(String email, String token, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Session session = new Session();
        session.setToken(token);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setUser(user);
        session.setExpiresAt(OffsetDateTime.now().plusDays(1));
        
        sessionRepository.save(session);
    }

    public void deleteSession(String token) {
        if (token != null) {
            sessionRepository.findByToken(token).ifPresent(sessionRepository::delete);
        }
    }
}
