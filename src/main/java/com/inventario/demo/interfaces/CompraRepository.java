package com.inventario.demo.interfaces;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventario.demo.modelo.Compra;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
}
