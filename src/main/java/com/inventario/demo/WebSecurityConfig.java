package com.inventario.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import com.inventario.demo.filter.JwtRequestFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	private final CorsConfig corsConfig;
	private final JwtRequestFilter jwtRequestFilter;
	private final UsuarioDetailsService usuarioDetailsService;

	public WebSecurityConfig(CorsConfig corsConfig, JwtRequestFilter jwtRequestFilter,
			UsuarioDetailsService usuarioDetailsService) {
		this.corsConfig = corsConfig;
		this.jwtRequestFilter = jwtRequestFilter;
		this.usuarioDetailsService = usuarioDetailsService;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(usuarioDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/usuario/login", "/api/usuario/refresh").permitAll()
						.anyRequest().authenticated())
				.authenticationProvider(authenticationProvider())
				.exceptionHandling(e -> e.authenticationEntryPoint(
						(request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}
