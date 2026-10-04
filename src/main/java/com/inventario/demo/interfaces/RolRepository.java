package com.inventario.demo.interfaces;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventario.demo.modelo.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

	Optional<Rol> findByNombreRol(String nombreRol);
}
