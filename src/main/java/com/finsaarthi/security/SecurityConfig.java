package com.finsaarthi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @org.springframework.beans.factory.annotation.Value("#{'${app.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                // ── Public auth endpoints ──────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/verify-registration-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/verify-forgot-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/update-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/captcha").permitAll()
                .requestMatchers(HttpMethod.GET, "/test/email").permitAll()

                // ── Public scholarship browsing ────────────────────────────
                .requestMatchers(HttpMethod.GET, "/scholarships").permitAll()
                .requestMatchers(HttpMethod.GET, "/scholarships/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/scholarships/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/scholarships/search").permitAll()

                // ── Admin-only scholarship management ──────────────────────
                .requestMatchers(HttpMethod.POST,   "/scholarships").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/scholarships/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/scholarships/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/scholarships/{id}/toggle").hasRole("ADMIN")

                // ── Admin-only user management ─────────────────────────────
                .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/users/stats").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/dashboard/stats").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/dashboard/trends").hasRole("ADMIN")

                // ── Admin-only application management ─────────────────────
                .requestMatchers(HttpMethod.GET,   "/applications").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,   "/applications/stats").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/applications/{id}/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/applications/{id}/documents/{documentId}/verification").hasRole("ADMIN")

                // ── Authenticated users (student + admin) ──────────────────
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                .requestMatchers("/users/{id}").authenticated()
                .requestMatchers("/users/{id}/profile").authenticated()
                .requestMatchers("/applications/my").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/applications").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET,  "/applications/{id}").authenticated()

                // ── All other requests require authentication ───────────────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
