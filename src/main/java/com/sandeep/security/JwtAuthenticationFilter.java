package com.sandeep.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String header = request.getHeader("Authorization");

        log.debug(">>> JWT Filter: {} {}", method, path);

        if (header == null || !header.startsWith("Bearer ")) {
            log.warn(">>> JWT Filter: No Bearer token on {} {} — header='{}'", method, path, header);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        log.debug(">>> JWT Filter: Token present (first 20 chars): {}...", token.substring(0, Math.min(20, token.length())));

        boolean valid = tokenProvider.isValid(token);
        log.debug(">>> JWT Filter: Token valid={}", valid);

        if (valid && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = tokenProvider.extractEmail(token);
            log.debug(">>> JWT Filter: Extracted email={}", email);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                log.debug(">>> JWT Filter: Loaded user={}, authorities={}", userDetails.getUsername(), userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug(">>> JWT Filter: Authentication set for {}", email);
            } catch (Exception e) {
                // User encoded in the JWT no longer exists in the database (e.g. account deleted)
                log.warn(">>> JWT Filter: Could not load user '{}' — {} (request will be rejected with 401)",
                        email, e.getMessage());
                // Do NOT set authentication; Spring Security will return 401 via the entry point
            }
        } else if (!valid) {
            log.warn(">>> JWT Filter: Token INVALID for {} {}", method, path);
        }

        filterChain.doFilter(request, response);
    }
}
