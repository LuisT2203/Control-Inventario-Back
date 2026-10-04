package com.inventario.demo.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventario.demo.Dto.AuthResponseDto;
import com.inventario.demo.Dto.LoginDTO;
import com.inventario.demo.interfaces.UsuarioRepository;
import com.inventario.demo.modelo.Usuario;
import com.inventario.demo.service.JwtUtilService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/usuario", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final UsuarioRepository usuarioRepository;
	private final JwtUtilService jwtUtilService;

	public UsuarioController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			UsuarioRepository usuarioRepository, JwtUtilService jwtUtilService) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.usuarioRepository = usuarioRepository;
		this.jwtUtilService = jwtUtilService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginDTO.getUsuario(), loginDTO.getClave()));
			return ResponseEntity.ok(tokens(loginDTO.getUsuario()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error Authentication");
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
		String refreshToken = request.get("refreshToken");
		try {
			String username = jwtUtilService.extractUsername(refreshToken);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			if (!jwtUtilService.validateToken(refreshToken, userDetails)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
			}
			return ResponseEntity.ok(tokens(username));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error Refresh Token");
		}
	}

	private AuthResponseDto tokens(String username) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		Usuario usuario = usuarioRepository.findByUsuario(username).orElseThrow();
		String tipo = usuario.getRol() == null ? "user" : usuario.getRol().getNombreRol().toLowerCase();
		AuthResponseDto respuesta = new AuthResponseDto();
		respuesta.setToken(jwtUtilService.generateToken(userDetails, tipo));
		respuesta.setRefreshToken(jwtUtilService.generateRefreshToken(userDetails, tipo));
		respuesta.setTipo(tipo);
		respuesta.setUsuario(username);
		return respuesta;
	}
}
