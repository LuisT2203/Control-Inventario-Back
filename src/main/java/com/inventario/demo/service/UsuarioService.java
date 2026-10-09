package com.inventario.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventario.demo.Dto.CambiarClaveDTO;
import com.inventario.demo.Dto.CrearUsuarioDTO;
import com.inventario.demo.Dto.UsuarioDTO;
import com.inventario.demo.interfaces.RolRepository;
import com.inventario.demo.interfaces.UsuarioRepository;
import com.inventario.demo.interfacesService.IUsuarioService;
import com.inventario.demo.modelo.Rol;
import com.inventario.demo.modelo.Usuario;
import com.inventario.demo.utils.ModeloNotFoundException;
import com.inventario.demo.utils.ReglaNegocioException;

/**
 * Gestion de usuarios (solo administrador). Todos entran con acceso total
 * (rol ADMIN); activar/desactivar corta hasta los tokens vigentes porque el
 * filtro y el refresh respetan el estado.
 */
@Service
public class UsuarioService implements IUsuarioService {

	private static final int CLAVE_MINIMA = 10;

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UsuarioDTO> listar() {
		exigirAdmin();
		return usuarioRepository.findAll().stream().map(this::aDto).toList();
	}

	@Override
	@Transactional
	public UsuarioDTO crear(CrearUsuarioDTO dto) {
		exigirAdmin();
		String nombre = nombreValido(dto.getUsuario());
		if (usuarioRepository.findByUsuario(nombre).isPresent()) {
			throw new ReglaNegocioException("Ese usuario ya existe", HttpStatus.CONFLICT);
		}
		Rol admin = rolRepository.findByNombreRol("ADMIN").orElseGet(() -> {
			Rol rol = new Rol();
			rol.setNombreRol("ADMIN");
			return rolRepository.save(rol);
		});
		Usuario usuario = new Usuario();
		usuario.setUsuario(nombre);
		usuario.setClave(passwordEncoder.encode(claveValida(dto.getClave())));
		usuario.setEstado(true);
		usuario.setRol(admin);
		return aDto(usuarioRepository.save(usuario));
	}

	@Override
	@Transactional
	public UsuarioDTO cambiarEstado(Integer id, boolean estado) {
		exigirAdmin();
		Usuario usuario = buscar(id);
		if (!estado && esPropio(usuario)) {
			throw new ReglaNegocioException("No puedes desactivarte a ti mismo", HttpStatus.BAD_REQUEST);
		}
		usuario.setEstado(estado);
		return aDto(usuarioRepository.save(usuario));
	}

	@Override
	@Transactional
	public UsuarioDTO cambiarClave(Integer id, CambiarClaveDTO dto) {
		exigirAdmin();
		Usuario usuario = buscar(id);
		usuario.setClave(passwordEncoder.encode(claveValida(dto == null ? null : dto.getClave())));
		return aDto(usuarioRepository.save(usuario));
	}

	private Usuario buscar(Integer id) {
		if (id == null) {
			throw new ReglaNegocioException("El usuario es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ModeloNotFoundException("Usuario no encontrado"));
	}

	private boolean esPropio(Usuario usuario) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && usuario.getUsuario().equalsIgnoreCase(auth.getName());
	}

	private String nombreValido(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new ReglaNegocioException("El usuario es obligatorio", HttpStatus.BAD_REQUEST);
		}
		String limpio = nombre.trim();
		if (limpio.length() < 3 || limpio.length() > 100) {
			throw new ReglaNegocioException("El usuario debe tener entre 3 y 100 caracteres",
					HttpStatus.BAD_REQUEST);
		}
		return limpio;
	}

	private String claveValida(String clave) {
		if (clave == null || clave.length() < CLAVE_MINIMA) {
			throw new ReglaNegocioException("La clave debe tener minimo " + CLAVE_MINIMA + " caracteres",
					HttpStatus.BAD_REQUEST);
		}
		return clave;
	}

	private void exigirAdmin() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		boolean admin = auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		if (!admin) {
			throw new ReglaNegocioException("Solo el administrador gestiona usuarios", HttpStatus.FORBIDDEN);
		}
	}

	private UsuarioDTO aDto(Usuario usuario) {
		UsuarioDTO dto = new UsuarioDTO();
		dto.setId(usuario.getId());
		dto.setUsuario(usuario.getUsuario());
		dto.setEstado(usuario.isEstado());
		dto.setRol(usuario.getRol() == null ? null : usuario.getRol().getNombreRol());
		return dto;
	}
}
