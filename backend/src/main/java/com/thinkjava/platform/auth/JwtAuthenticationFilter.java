package com.thinkjava.platform.auth;

import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwt;
  private final UserRepository users;

  public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
    this.jwt = jwt;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);

      // Only authenticate if SecurityContext is empty or anonymous
      Authentication existing = SecurityContextHolder.getContext().getAuthentication();
      boolean alreadyHasRealUser =
          existing != null
              && existing.isAuthenticated()
              && !(existing instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);

      if (!alreadyHasRealUser) {
        try {
          String email = jwt.validateAndGetSubject(token);

          users.findByEmail(email).ifPresent(user -> {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          });
        } catch (Exception ignored) {
          // Invalid/expired token -> leave unauthenticated
        }
      }
    }

    filterChain.doFilter(request, response);
  }

}
