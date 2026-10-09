package com.inventario.demo.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventario.demo.Dto.AuthResponseDto;
import com.inventario.demo.Dto.CambiarClaveDTO;
import com.inventario.demo.Dto.CambiarEstadoDTO;
import com.inventario.demo.Dto.CrearUsuarioDTO;
import com.inventario.demo.Dto.LoginDTO;
import com.inventario.demo.interfaces.UsuarioRepository;
import com.inventario.demo.interfacesService.IUsuarioService;
import com.inventario.demo.modelo.Usuario;
import com.inventario.demo.service.JwtUtilService;
import com.inventario.demo.service.LoginAttemptService;
import com.inventario.demo.utils.MensajeResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/usuario", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final UsuarioRepository usuarioRepository;
	private final JwtUtilService jwtUtilService;
	private final LoginAttemptService loginAttemptService;
	private final IUsuarioService usuarioService;

	public UsuarioController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			UsuarioRepository usuarioRepository, JwtUtilService jwtUtilService,
			LoginAttemptService loginAttemptService, IUsuarioService usuarioService) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.usuarioRepository = usuarioRepository;
		this.jwtUtilService = jwtUtilService;
		this.loginAttemptService = loginAttemptService;
		this.usuarioService = usuarioService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
		String clave = loginDTO.getUsuario() + "|" + ipCliente(request);
		if (loginAttemptService.bloqueado(clave)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Error Authentication");
		}
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginDTO.getUsuario(), loginDTO.getClave()));
			loginAttemptService.registrarExito(clave);
			return ResponseEntity.ok(tokens(loginDTO.getUsuario()));
		} catch (Exception e) {
			loginAttemptService.registrarFallo(clave);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error Authentication");
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
		String refreshToken = request.get("refreshToken");
		try {
			String username = jwtUtilService.extractUsername(refreshToken);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			if (!userDetails.isEnabled() || !jwtUtilService.validateToken(refreshToken, userDetails)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
			}
			return ResponseEntity.ok(tokens(username));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error Refresh Token");
		}
	}

	private String ipCliente(HttpServletRequest request) {
		String reenviada = request.getHeader("X-Forwarded-For");
		if (reenviada != null && !reenviada.isBlank()) {
			return reenviada.split(",")[0].trim();
		}
		return request.getRemoteAddr();
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

	@GetMapping("/listar")
	public ResponseEntity<?> listar() {
		return ResponseEntity.ok(usuarioService.listar());
	}

	@PostMapping("/crear")
	public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuarioDTO dto) {
		return new ResponseEntity<>(usuarioService.crear(dto), HttpStatus.CREATED);
	}

	@PutMapping("/estado/{id}")
	public ResponseEntity<?> estado(@PathVariable Integer id, @RequestBody CambiarEstadoDTO dto) {
		return ResponseEntity.ok(usuarioService.cambiarEstado(id, dto != null && dto.isEstado()));
	}

	@PutMapping("/clave/{id}")
	public ResponseEntity<?> clave(@PathVariable Integer id, @Valid @RequestBody CambiarClaveDTO dto) {
		return ResponseEntity.ok(usuarioService.cambiarClave(id, dto));
	}
}
