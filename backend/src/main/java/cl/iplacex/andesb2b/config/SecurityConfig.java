package cl.iplacex.andesb2b.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.requireExplicitSave(false))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/css/**",
                        "/js/**",
                        "/pantallas/**",
                        "/favicon.ico",
                        "/api/auth/**"
                ).permitAll()
                .requestMatchers("/api/usuarios/**", "/api/roles/**", "/api/catalogo/auditoria")
                    .hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"Sesión expirada o no autenticada. Inicia sesión nuevamente.\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"Acceso denegado: tu rol no tiene permisos para esta operación.\"}");
                })
            );
        return http.build();
    }
}