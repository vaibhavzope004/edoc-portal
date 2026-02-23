package com.edoc.portal.config;

import com.edoc.portal.entity.User;
import com.edoc.portal.enums.Role;
import com.edoc.portal.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.util.Locale;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return email -> {

            String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            boolean enabled = "ACTIVE".equalsIgnoreCase(user.getStatus())
                    || "APPROVED".equalsIgnoreCase(user.getStatus())
                    || user.getRole() == Role.ADMIN;

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .disabled(!enabled)
                    .build();
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/", "/login", "/register",
                            "/admin/login", "/csc/login", "/csc/register",
                            "/css/**", "/images/**"
                    ).permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/csc/**").hasRole("CSC")
                    .requestMatchers("/customer/**").hasRole("CUSTOMER")
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(roleBasedSuccessHandler())
                    .failureHandler((request, response, exception) -> {
                        String portal = request.getParameter("portal");
                        if ("admin".equalsIgnoreCase(portal)) {
                            response.sendRedirect("/admin/login?error");
                            return;
                        }
                        if ("csc".equalsIgnoreCase(portal)) {
                            response.sendRedirect("/csc/login?error");
                            return;
                        }
                        response.sendRedirect("/login?error");
                    })
                    .permitAll()
            )
            .exceptionHandling(ex -> ex
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) -> response.sendRedirect("/admin/login"),
                            request -> request.getRequestURI() != null && request.getRequestURI().startsWith("/admin/")
                    )
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) -> response.sendRedirect("/csc/login"),
                            request -> request.getRequestURI() != null && request.getRequestURI().startsWith("/csc/")
                    )
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) -> response.sendRedirect("/login"),
                            request -> true
                    )
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
            )
            .headers(headers -> headers
                    .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {

            String portal = request.getParameter("portal");

            if ("admin".equalsIgnoreCase(portal) && !hasRole(authentication, "ROLE_ADMIN")) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect("/admin/login?invalidAdminUser");
                return;
            }

            if ("csc".equalsIgnoreCase(portal) && !hasRole(authentication, "ROLE_CSC")) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect("/csc/login?invalidCscUser");
                return;
            }

            if ("customer".equalsIgnoreCase(portal) && !hasRole(authentication, "ROLE_CUSTOMER")) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect("/login?invalidCustomerUser");
                return;
            }

            if (hasRole(authentication, "ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
                return;
            }

            if (hasRole(authentication, "ROLE_CSC")) {
                response.sendRedirect("/csc/dashboard");
                return;
            }

            response.sendRedirect("/customer/dashboard");
        };
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
