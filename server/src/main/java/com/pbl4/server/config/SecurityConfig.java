package com.pbl4.server.config; 


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
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // For CORS Bean

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider; 


    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          ObjectMapper objectMapper,
                          JwtTokenProvider jwtTokenProvider) { 
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider; 
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

        JwtAuthenticationFilter customAuthFilter = new JwtAuthenticationFilter(authenticationManager, jwtTokenProvider, objectMapper);
        customAuthFilter.setFilterProcessesUrl("/api/auth/login");

        http
            .csrf(csrf -> csrf.disable()) 
            .cors(Customizer.withDefaults()) 
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            )
            .exceptionHandling(exceptions -> exceptions
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
            				"/",
            				"/login.html", 
            		        "/register.html", 
            		        "/forgot-password.html",
            		        "/reset-password.html",
            		        "/live_feed.html", 
            		        "/snapshot_management.html",
            		        "/client_setup.html",
            		        "/user_settings.html",
            		        "/stream.html",
            		        "/assets/**", 
            		        "/admin/**",
            		        "/camera_list.html",
            		        "/camera_gallery.html",
            		        "/change_password.html",
            		        "/verify_success.html"
            		        
            		    ).permitAll()
            	.requestMatchers("/ws/updates/**").permitAll()
                .requestMatchers("/api/auth/**", "/error", "/assets/**").permitAll()
                // **Allow viewing images publicly (if desired)** - Place specific rules first
                .requestMatchers(HttpMethod.PUT, "/api/users/{id}").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/*/request-email-change").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/verify_email.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-email-change").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/verify-email-change").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/images/view/**").permitAll()       
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/api/auth/change-password").authenticated()
                .anyRequest().authenticated()
            )
       
            .addFilter(customAuthFilter)

            .addFilterBefore(new JwtAuthorizationFilter(jwtTokenProvider, userDetailsService),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean for global CORS configuration (can be in a separate WebConfig class too)
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
        	@Override
            public void addViewControllers(ViewControllerRegistry registry) {
                
                registry.addViewController("/")
                        .setViewName("forward:/login.html");
            }
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") 
                        .allowedOrigins("http://127.0.0.1:5500","http://localhost:8080","http://192.168.110.236:8080") 
                        //.allowedOrigins("**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 
                        .allowedHeaders("*")
                        .allowCredentials(true); 
            }
        };
    }
}