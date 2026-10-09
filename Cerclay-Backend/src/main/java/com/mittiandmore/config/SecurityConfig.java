package com.mittiandmore.config;

import com.mittiandmore.service.AdminUserDetailsService;
import com.mittiandmore.service.CustomerUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    private final CustomerUserDetailsService customerUserDetailsService;
    private final AdminUserDetailsService adminUserDetailsService;

    public SecurityConfig(
        CustomerUserDetailsService customerUserDetailsService,
        AdminUserDetailsService adminUserDetailsService
    ) {
        this.customerUserDetailsService = customerUserDetailsService;

        this.adminUserDetailsService = adminUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "customerAuthenticationManager")
    @Primary
    public AuthenticationManager customerAuthenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customerUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean(name = "adminAuthenticationManager")
    public AuthenticationManager adminAuthenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(adminUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DualClientSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository
    ) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) ->
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");

        http.securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)

            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))

            .authorizeHttpRequests(auth ->
                auth

                    .requestMatchers("/uploads/products/**", "/uploads/returns/**")
                    .permitAll()

                    .requestMatchers(
                        "/api/payments/razorpay/webhook",
                        "/api/payments/cashfree/webhook",
                        "/api/webhooks/shadowfax",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/otp/send",
                        "/api/auth/otp/verify",
                        "/api/auth/otp/password-reset",
                        "/api/auth/google",
                        "/error"
                    )
                    .permitAll()

                    .requestMatchers("/api/admin/login")
                    .permitAll()

                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                    .requestMatchers(HttpMethod.GET, "/api/products/**")
                    .permitAll()

                    .requestMatchers(HttpMethod.POST, "/api/shipping/quote")
                    .permitAll()

                    .requestMatchers(HttpMethod.GET, "/api/discounts/available")
                    .permitAll()

                    .requestMatchers(HttpMethod.POST, "/api/products/**")
                    .hasRole("ADMIN")

                    .requestMatchers(HttpMethod.PUT, "/api/products/**")
                    .hasRole("ADMIN")

                    .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                    .hasRole("ADMIN")

                    .requestMatchers("/api/cart/**")
                    .permitAll()

                    .requestMatchers("/api/wishlist/**")
                    .authenticated()

                    .requestMatchers("/api/recently-viewed/**")
                    .authenticated()

                    .requestMatchers(HttpMethod.GET, "/api/reviews/featured")
                    .permitAll()

                    .requestMatchers(HttpMethod.GET, "/api/reviews/mine")
                    .authenticated()

                    .requestMatchers(HttpMethod.GET, "/api/reviews/product/*/eligibility")
                    .authenticated()

                    .requestMatchers(HttpMethod.GET, "/api/reviews/product/**")
                    .permitAll()

                    .requestMatchers("/api/reviews/product/**")
                    .authenticated()

                    .requestMatchers("/api/reviews/admin/**")
                    .hasRole("ADMIN")

                    .requestMatchers("/api/customers")
                    .hasRole("ADMIN")

                    .requestMatchers("/api/customers/**")
                    .authenticated()

                    .requestMatchers("/api/addresses/**")
                    .authenticated()

                    .requestMatchers(HttpMethod.GET, "/api/store-settings")
                    .permitAll()

                    .requestMatchers(HttpMethod.PUT, "/api/store-settings")
                    .hasRole("ADMIN")

                    .anyRequest()
                    .authenticated()
            );

        return http.build();
    }
}
