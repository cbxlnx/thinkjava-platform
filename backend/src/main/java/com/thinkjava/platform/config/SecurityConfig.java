package com.thinkjava.platform.config;

import com.thinkjava.platform.auth.JwtAuthenticationFilter;
import com.thinkjava.platform.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Security configuration class
@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtFilter;
  private final UserRepository userRepo;

  public SecurityConfig(JwtAuthenticationFilter jwtFilter, UserRepository userRepo) {
    this.jwtFilter = jwtFilter;
    this.userRepo = userRepo;
  }
  // password encoder bean
  @Bean
  public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  // user details service bean for authentication
  @Bean
  public UserDetailsService userDetailsService() {
    return username -> userRepo.findByEmail(username).orElseThrow();
  }
  // authentication provider bean that uses the user details service and password encoder
  @Bean
  public DaoAuthenticationProvider authProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService());
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }
  // authentication manager bean
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }
  // security filter chain configuration
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(c -> {})
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/diagnostic/**").authenticated()
        .requestMatchers("/api/learn/**").authenticated() 
        .requestMatchers(
            "/api/auth/**",
            "/api/ping",
            "/error",
            "/"
        ).permitAll()  // public endpoints
        .anyRequest().authenticated()                               // everything else requires JWT
      )
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
        .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
      )
      .authenticationProvider(authProvider())                                   // add provider
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)   // add JWT filter
      .httpBasic(h -> h.disable())
      .formLogin(f -> f.disable());

    return http.build();
  }
}
