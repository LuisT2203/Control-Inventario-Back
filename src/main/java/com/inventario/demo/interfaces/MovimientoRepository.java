package com.inventario.demo.interfaces;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.inventario.demo.modelo.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

	boolean existsByProductoIdProducto(Integer idProducto);

	List<Movimiento> findByProductoIdProductoOrderByFechaHoraAscIdMovimientoAsc(Integer idProducto);

	@Query("""
			select m from Movimiento m
			where m.producto.local.idLocal = :idLocal
			and m.fechaHora >= :desde
			and (:idProducto is null or m.producto.idProducto = :idProducto)
			order by m.fechaHora desc
			""")
	List<Movimiento> findRecientes(Integer idLocal, java.time.LocalDateTime desde, Integer idProducto,
			org.springframework.data.domain.Pageable pageable);

	List<Movimiento> findByVentaIdVentaOrderByIdMovimientoAsc(Integer idVenta);

	List<Movimiento> findByCompraIdCompraOrderByIdMovimientoAsc(Integer idCompra);
}
