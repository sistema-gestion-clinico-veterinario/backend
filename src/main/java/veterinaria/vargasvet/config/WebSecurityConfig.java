package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import veterinaria.vargasvet.security.JWTFilter;
import veterinaria.vargasvet.security.JwtAuthenticationEntryPoint;
import veterinaria.vargasvet.security.CookieSecurityFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JWTFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final veterinaria.vargasvet.security.RateLimitFilter rateLimitFilter;
    private final CookieSecurityFilter cookieSecurityFilter;

    @Value("${cors.allowed-origins:https://systemvetfrontend.vercel.app,http://localhost:4200}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // Permitir preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos
                        .requestMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/auth/resend-verification",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/email-change/confirm-current",
                                "/auth/email-change/confirm-new",
                                "/auth/validate-reset-token",
                                "/health",
                                "/setup/**",
                                "/auth/setup-account",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws/**",
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/media/**").permitAll()

                        .requestMatchers("/actuator/**").denyAll()

                        // Todo lo demás requiere auth
                        .anyRequest().authenticated()
                )

                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()"))
                );

        // Registrar primero JWT contra un filtro estándar con orden conocido.
        // Los filtros personalizados pueden referenciarlo después sin depender
        // del orden en que Spring descubra sus beans.
        http.addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        // Validar el origen de las cookies antes de procesar la autenticación.
        http.addFilterBefore(cookieSecurityFilter, JWTFilter.class);

        // Rate Limit Filter (despues de JWT, para poder limitar por usuario autenticado)
        http.addFilterAfter(
                rateLimitFilter,
                JWTFilter.class
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .filter(o -> !o.isBlank())
                .collect(Collectors.toList());
        if (origins.isEmpty() || origins.contains("*")
                || origins.stream().anyMatch(o -> !(o.startsWith("https://") || o.startsWith("http://")))) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS debe contener orígenes HTTP(S) explícitos y no admite comodines");
        }
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("Accept", "Content-Type", "Authorization", "X-Requested-With"));

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<JWTFilter> jwtFilterRegistration(JWTFilter filter) {
        return securityFilterRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<CookieSecurityFilter> cookieSecurityFilterRegistration(
            CookieSecurityFilter filter) {
        return securityFilterRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<veterinaria.vargasvet.security.RateLimitFilter> rateLimitFilterRegistration(
            veterinaria.vargasvet.security.RateLimitFilter filter) {
        return securityFilterRegistration(filter);
    }

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> securityFilterRegistration(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder(
            @org.springframework.beans.factory.annotation.Value("${security.bcrypt-strength:12}") int strength) {
        if (strength < 10 || strength > 14) {
            throw new IllegalStateException("security.bcrypt-strength debe estar entre 10 y 14");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }
}
