package com.luisdev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identity echoed back on login/validate.
 *
 * {@code role} exists so the Angular client can decide what to OFFER — the
 * cross-user analytics scope, for one. It is never what decides what is
 * ALLOWED: the authoritative role lives in the JWT's `authorities` claim and
 * is checked server-side, here and again in kubo-analytics.
 *
 * Every field but {@code message} is nullable: logout and the 401 branch reuse
 * this shape with no user attached.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String message;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
