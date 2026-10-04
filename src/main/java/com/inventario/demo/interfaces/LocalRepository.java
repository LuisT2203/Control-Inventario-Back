package com.inventario.demo.interfaces;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventario.demo.modelo.Local;

public interface LocalRepository extends JpaRepository<Local, Integer> {

	Optional<Local> findByCodigo(String codigo);
}
