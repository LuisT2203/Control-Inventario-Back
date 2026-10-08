package com.inventario.demo.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.interfaces.RolRepository;
import com.inventario.demo.interfaces.UsuarioRepository;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.modelo.Rol;
import com.inventario.demo.modelo.Usuario;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;
	private final LocalRepository localRepository;
	private final PasswordEncoder passwordEncoder;

	public DatosIniciales(RolRepository rolRepository, UsuarioRepository usuarioRepository,
			LocalRepository localRepository, PasswordEncoder passwordEncoder) {
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.localRepository = localRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		Rol admin = rolRepository.findByNombreRol("ADMIN").orElseGet(() -> {
			Rol rol = new Rol();
			rol.setNombreRol("ADMIN");
			return rolRepository.save(rol);
		});
		crearUsuario("rosita", "rosita123", admin);
		crearUsuario("mary", "mary123", admin);
		if (localRepository.count() > 0) {
			return;
		}
		crearLocal("UNIFORMES", "Uniformes", true);
		crearLocal("RELIGIOSOS", "Articulos religiosos", false);
	}

	private void crearUsuario(String nombre, String clave, Rol rol) {
		if (usuarioRepository.findByUsuario(nombre).isPresent()) {
			return;
		}
		Usuario usuario = new Usuario();
		usuario.setUsuario(nombre);
		usuario.setClave(passwordEncoder.encode(clave));
		usuario.setEstado(true);
		usuario.setRol(rol);
		usuarioRepository.save(usuario);
	}

	private Local crearLocal(String codigo, String nombre, boolean exigeTalla) {
		Local local = new Local();
		local.setCodigo(codigo);
		local.setNombre(nombre);
		local.setExigeTalla(exigeTalla);
		return localRepository.save(local);
	}
}
