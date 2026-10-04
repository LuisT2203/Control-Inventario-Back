package com.inventario.demo.interfaces;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventario.demo.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

	Optional<Usuario> findByUsuario(String usuario);
}
