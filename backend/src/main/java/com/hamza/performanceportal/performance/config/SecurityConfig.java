package com.hamza.performanceportal.performance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the application
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.user.name}")
    private String securityUserName;

    @Value("${app.security.user.password}")
    private String securityUserPassword;

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${app.security.cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Value("${app.security.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicy(policy -> policy
                                .policy("geolocation=(), microphone=(), camera=()")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(securityUserName)
                .password(passwordEncoder.encode(securityUserPassword))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseCsv(corsAllowedOrigins));
        configuration.setAllowedMethods(parseCsv(corsAllowedMethods));
        configuration.setAllowedHeaders(parseCsv(corsAllowedHeaders));
        configuration.setAllowCredentials(corsAllowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.toList());
    }
}

// Made with Bob
