package com.inventario.demo.service;

import org.springframework.beans.factory.annotation.Value;
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

/**
 * Siembra inicial SOLO para produccion (app.seed-demo-data=false): crea el rol
 * ADMIN, los dos locales de la tienda y el usuario administrador cuyas
 * credenciales vienen de variables de entorno. No crea usuarios demo.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "false")
public class AdminInicial implements CommandLineRunner {

	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;
	private final LocalRepository localRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${ADMIN_USER:}")
	private String adminUser;

	@Value("${ADMIN_PASSWORD:}")
	private String adminPassword;

	public AdminInicial(RolRepository rolRepository, UsuarioRepository usuarioRepository,
			LocalRepository localRepository, PasswordEncoder passwordEncoder) {
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.localRepository = localRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (localRepository.count() == 0) {
			crearLocal("UNIFORMES", "Uniformes", true);
			crearLocal("RELIGIOSOS", "Articulos religiosos", false);
		}
		rolRepository.findByNombreRol("ADMIN").orElseGet(() -> {
			Rol rol = new Rol();
			rol.setNombreRol("ADMIN");
			return rolRepository.save(rol);
		});
		if (usuarioRepository.count() > 0) {
			return;
		}
		if (adminUser.isBlank() || adminPassword == null || adminPassword.length() < 10) {
			throw new IllegalStateException(
					"Base sin usuarios: define ADMIN_USER y ADMIN_PASSWORD (minimo 10 caracteres) para crear el administrador inicial.");
		}
		Rol admin = rolRepository.findByNombreRol("ADMIN").orElseThrow();
		if (usuarioRepository.findByUsuario(adminUser).isPresent()) {
			return;
		}
		Usuario usuario = new Usuario();
		usuario.setUsuario(adminUser);
		usuario.setClave(passwordEncoder.encode(adminPassword));
		usuario.setEstado(true);
		usuario.setRol(admin);
		usuarioRepository.save(usuario);
	}

	private void crearLocal(String codigo, String nombre, boolean exigeTalla) {
		Local local = new Local();
		local.setCodigo(codigo);
		local.setNombre(nombre);
		local.setExigeTalla(exigeTalla);
		localRepository.save(local);
	}
}
