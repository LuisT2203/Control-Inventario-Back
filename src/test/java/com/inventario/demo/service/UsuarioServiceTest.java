package com.inventario.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventario.demo.Dto.CambiarClaveDTO;
import com.inventario.demo.Dto.CrearUsuarioDTO;
import com.inventario.demo.Dto.UsuarioDTO;
import com.inventario.demo.interfaces.RolRepository;
import com.inventario.demo.interfaces.UsuarioRepository;
import com.inventario.demo.modelo.Rol;
import com.inventario.demo.modelo.Usuario;
import com.inventario.demo.utils.ReglaNegocioException;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private RolRepository rolRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	private UsuarioService service;

	@BeforeEach
	void setUp() {
		service = new UsuarioService(usuarioRepository, rolRepository, passwordEncoder);
		comoAdmin("rosita");
	}

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void crearGuardaActivoYConClaveCifrada() {
		when(usuarioRepository.findByUsuario("maria")).thenReturn(Optional.empty());
		when(rolRepository.findByNombreRol("ADMIN")).thenReturn(Optional.of(rol()));
		when(passwordEncoder.encode("clave-segura-1")).thenReturn("HASH");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		CrearUsuarioDTO dto = new CrearUsuarioDTO();
		dto.setUsuario("maria");
		dto.setClave("clave-segura-1");
		UsuarioDTO creado = service.crear(dto);

		assertEquals("maria", creado.getUsuario());
		assertTrue(creado.isEstado());
		assertEquals("ADMIN", creado.getRol());
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		assertEquals("HASH", captor.getValue().getClave());
	}

	@Test
	void crearDuplicadoSeRechaza() {
		when(usuarioRepository.findByUsuario("maria")).thenReturn(Optional.of(usuario(2, "maria", true)));

		CrearUsuarioDTO dto = new CrearUsuarioDTO();
		dto.setUsuario("maria");
		dto.setClave("clave-segura-1");

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.crear(dto));
		assertEquals(HttpStatus.CONFLICT, error.getStatus());
	}

	@Test
	void crearClaveCortaSeRechaza() {
		when(usuarioRepository.findByUsuario("maria")).thenReturn(Optional.empty());
		when(rolRepository.findByNombreRol("ADMIN")).thenReturn(Optional.of(rol()));

		CrearUsuarioDTO dto = new CrearUsuarioDTO();
		dto.setUsuario("maria");
		dto.setClave("corta");

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.crear(dto));
		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void sinRolAdminSeRechaza() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y",
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.listar());
		assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
	}

	@Test
	void noPuedesDesactivarteATiMismo() {
		when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario(1, "rosita", true)));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.cambiarEstado(1, false));
		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void desactivarAOtroFunciona() {
		when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario(2, "maria", true)));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		UsuarioDTO dto = service.cambiarEstado(2, false);

		assertEquals("maria", dto.getUsuario());
		assertTrue(!dto.isEstado());
	}

	@Test
	void cambiarClaveCifraLaNueva() {
		when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario(2, "maria", true)));
		when(passwordEncoder.encode("nueva-clave-22")).thenReturn("HASH2");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		CambiarClaveDTO dto = new CambiarClaveDTO();
		dto.setClave("nueva-clave-22");
		service.cambiarClave(2, dto);

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		assertEquals("HASH2", captor.getValue().getClave());
	}

	private void comoAdmin(String nombre) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(nombre, "x",
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}

	private Rol rol() {
		Rol rol = new Rol();
		rol.setNombreRol("ADMIN");
		return rol;
	}

	private Usuario usuario(int id, String nombre, boolean estado) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setUsuario(nombre);
		usuario.setClave("HASH");
		usuario.setEstado(estado);
		usuario.setRol(rol());
		return usuario;
	}
}
