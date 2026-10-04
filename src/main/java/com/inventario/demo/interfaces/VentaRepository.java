package com.inventario.demo.interfaces;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventario.demo.modelo.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
}
