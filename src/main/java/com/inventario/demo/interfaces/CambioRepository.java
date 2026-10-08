package com.inventario.demo.interfaces;

import com.inventario.demo.modelo.Cambio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioRepository extends JpaRepository<Cambio, Integer> {
}
