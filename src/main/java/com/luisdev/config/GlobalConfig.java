package com.luisdev.config;

import com.luisdev.security.InternalTokenFilter;
import com.luisdev.security.JwtValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
public class GlobalConfig {
  private final JwtValidator jwtValidator;
  private final InternalTokenFilter internalTokenFilter;
  private final String corsAllowedOrigin;

  public GlobalConfig(JwtValidator jwtValidator, InternalTokenFilter internalTokenFilter,
      @Value("${cors.allowed-origin}") String corsAllowedOrigin) {
    this.jwtValidator = jwtValidator;
    this.internalTokenFilter = internalTokenFilter;
    this.corsAllowedOrigin = corsAllowedOrigin;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .exceptionHandling(handling -> handling
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.getWriter()
                  .write("{\"error\": \"No autorizado\", \"message\": \"Sesión inválida o inexistente\"}");
            }))
        .sessionManagement(managment -> managment.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .httpBasic(Customizer.withDefaults())
        .formLogin(form -> form.disable())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()) // Permitimos todos los endpoints de momento
        // Order matters, and the reason is easy to miss: because the rule above
        // is permitAll, authorizeHttpRequests guards nothing. The ONLY thing
        // standing between the outside world and /api/internal/** is this
        // filter. Remove it and the whole history table becomes public.
        .addFilterBefore(internalTokenFilter, BasicAuthenticationFilter.class)
        .addFilterBefore(jwtValidator, BasicAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of(corsAllowedOrigin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("New-Token"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // Most specific pattern wins. An empty config allows no origin at all, so
    // a browser can never be handed cross-origin permission for the internal
    // API — kubo-analytics calls it server-to-server and sends no Origin
    // header, which the CORS filter passes straight through.
    source.registerCorsConfiguration("/api/internal/**", new CorsConfiguration());
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
