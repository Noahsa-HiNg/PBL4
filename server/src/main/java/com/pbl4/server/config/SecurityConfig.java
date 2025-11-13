package com.pbl4.server.config; // Ensure correct package

// --- Necessary Imports ---
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.security.JwtAuthenticationFilter; // Your login filter
import com.pbl4.server.security.JwtAuthorizationFilter; // **REQUIRED: Filter to check token**
import com.pbl4.server.security.JwtTokenProvider;      // **REQUIRED: Service to manage tokens**
import com.pbl4.server.service.UserDetailsServiceImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // For STATELESS
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // For 401 response
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Reference point
import org.springframework.web.servlet.config.annotation.CorsRegistry; // For CORS Bean
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // For CORS Bean

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Use final fields and constructor injection (recommended)
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider; // **Inject Token Provider**

    // Constructor for dependency injection
    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          ObjectMapper objectMapper,
                          JwtTokenProvider jwtTokenProvider) { // **Add JwtTokenProvider**
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider; // **Initialize Token Provider**
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        // Instantiate the LOGIN filter, passing required dependencies
        JwtAuthenticationFilter customAuthFilter = new JwtAuthenticationFilter(authenticationManager, jwtTokenProvider, objectMapper);
        // Set the URL this filter processes
        customAuthFilter.setFilterProcessesUrl("/api/auth/login");

        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless JWT
            .cors(Customizer.withDefaults()) // Apply CORS configuration from the WebMvcConfigurer Bean below
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // **Crucial for JWT**
            )
            .exceptionHandling(exceptions -> exceptions
                // Custom handler for 401 Unauthorized errors (when token is missing/invalid)
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Yêu cầu xác thực. Vui lòng đăng nhập.");
                    data.put("error", "Unauthorized");
                    response.getWriter().write(objectMapper.writeValueAsString(data));
                })
            )
            .authorizeHttpRequests(auth -> auth
            		.requestMatchers(
            		        "/login.html", 
            		        "/register.html"
            		        
            		    ).permitAll()
                // Allow login, error pages, and static assets publicly
            	.requestMatchers("/ws/updates/**").permitAll()
                .requestMatchers("/api/auth/**", "/error", "/assets/**").permitAll()
                // **Allow viewing images publicly (if desired)** - Place specific rules first
                //.requestMatchers(HttpMethod.PUT, "/api/users/{id}").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/images/view/**").permitAll()
        
                // All other /api/** endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                // Any other request also requires authentication
                .anyRequest().authenticated()
            )
            // **ADD THE FILTERS IN THE CORRECT ORDER**
            // 1. Add the custom LOGIN filter (replaces the default form login filter)
            .addFilter(customAuthFilter)
            // 2. Add the custom AUTHORIZATION filter (checks token on other requests)
            //    It runs BEFORE the standard UsernamePasswordAuthenticationFilter
            .addFilterBefore(new JwtAuthorizationFilter(jwtTokenProvider, userDetailsService),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean for global CORS configuration (can be in a separate WebConfig class too)
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Apply to all API paths
                        .allowedOrigins("http://127.0.0.1:5500","http://localhost:8080","http://10.115.71.111:5500") 
                        //.allowedOrigins("**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers (including Authorization)
                        .allowCredentials(true); // Allow sending credentials (like tokens)
            }
        };
    }
}