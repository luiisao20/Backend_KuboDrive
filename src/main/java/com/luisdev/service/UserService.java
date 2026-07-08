package com.luisdev.service;

import com.luisdev.domain.entity.User;
import com.luisdev.domain.enums.Role;
import com.luisdev.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User registerUser(User user) {
    if (userRepository.findByEmail(user.getEmail()).isPresent()) {
      throw new RuntimeException("El email ya está registrado");
    }
    user.setEmail(user.getEmail().toLowerCase());
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    if (user.getRole() == null) {
      user.setRole(Role.ROLE_USER);
    }
    return userRepository.save(user);
  }

  public java.util.UUID findUserIdByEmail(String email) {
    return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found")).getId();
  }

  public void updatePassword(String email, String oldPassword, String newPassword) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
        throw new BadCredentialsException("La contraseña actual es incorrecta");
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  public Authentication login(String email, String password, String ip) {
    try {
      UserDetails userDetails = loadUserByUsername(email);
      if (passwordEncoder.matches(password, userDetails.getPassword())) {
        return new UsernamePasswordAuthenticationToken(userDetails, ip, userDetails.getAuthorities());
      }
      throw new BadCredentialsException("Error de autenticación");
    } catch (Exception e) {
      throw new BadCredentialsException("Error de autenticación", e);
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    return org.springframework.security.core.userdetails.User
        .builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .authorities(user.getRole().name())
        .build();
  }
}
