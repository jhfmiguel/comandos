package com.comandos.security.config;

import com.comandos.security.service.AccountService;
import com.comandos.security.service.AccessPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AccountService accounts, AccessPolicy access,
            @Value("${platform.security.require-login:false}") boolean requireLogin,
            @Value("${platform.allowed-origin:http://localhost:3000}") String allowedOrigin) throws Exception {
        var provider = new DaoAuthenticationProvider(accounts);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(allowedOrigin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        http.securityMatcher("/api/**")
            .authenticationManager(new ProviderManager(provider))
            .cors(config -> config.configurationSource(source))
            .csrf(config -> {
                // Keep the existing registration workflow usable until an account is provisioned.
                // Authentication endpoints always require CSRF protection, including login/logout.
                if (!requireLogin) config.ignoringRequestMatchers(request -> !request.getServletPath().startsWith("/api/auth/"));
            })
            .requestCache(config -> config.disable())
            .authorizeHttpRequests(config -> {
                config.requestMatchers("/api/auth/session", "/api/auth/csrf", "/api/auth/login").permitAll();
                if (requireLogin) config.anyRequest().access((authentication, context) -> new AuthorizationDecision(
                    authentication.get().isAuthenticated() && !(authentication.get() instanceof AnonymousAuthenticationToken)
                        && access.legacyAllowed(context.getRequest().getServletPath(), context.getRequest().getMethod())));
                else config.anyRequest().permitAll();
            })
            .formLogin(config -> config.loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> response.setStatus(204))
                .failureHandler((request, response, exception) -> problem(response, 401, "Invalid login or password.")))
            .logout(config -> config.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .exceptionHandling(config -> config
                .authenticationEntryPoint((request, response, exception) -> problem(response, 401, "Sign in to continue."))
                .accessDeniedHandler((request, response, exception) -> problem(response, 403, "Access denied or session token expired. Refresh and try again.")))
            .addFilterBefore(new SessionAccountFilter(accounts), AuthorizationFilter.class);
        return http.build();
    }

    private static void problem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
