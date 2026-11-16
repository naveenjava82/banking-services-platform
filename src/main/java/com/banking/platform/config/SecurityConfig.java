
package com.banking.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Configures authentication and authorization for the Banking Services Platform.
 */
@Configuration
public class SecurityConfig {

    /**
     * Creates a BCrypt password encoder.
     *
     * @return the password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures in-memory demo users with customer and admin roles.
     *
     * @param encoder password encoder
     * @param first first customer password
     * @param second second customer password
     * @param admin administrator password
     * @return configured user details service
     */
    @Bean
    UserDetailsService users(
            PasswordEncoder encoder,
            @Value("${app.demo-users.customer1-password}") String first,
            @Value("${app.demo-users.customer2-password}") String second,
            @Value("${app.demo-users.admin-password}") String admin) {

        return new InMemoryUserDetailsManager(
                User.withUsername("customer-1001")
                        .password(encoder.encode(first))
                        .roles("CUSTOMER")
                        .build(),
                User.withUsername("customer-1002")
                        .password(encoder.encode(second))
                        .roles("CUSTOMER")
                        .build(),
                User.withUsername("admin")
                        .password(encoder.encode(admin))
                        .roles("ADMIN")
                        .build()
        );
    }

    /**
     * Configures stateless HTTP Basic authentication and role-based API access.
     *
     * @param http HTTP security configuration
     * @return configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/**")
                        .hasRole("CUSTOMER")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
