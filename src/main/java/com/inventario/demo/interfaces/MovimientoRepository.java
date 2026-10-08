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

	List<Movimiento> findByCambioIdCambioOrderByIdMovimientoAsc(Integer idCambio);

	@Query("""
			select coalesce(sum(m.cantidad), 0) from Movimiento m
			where m.cambio is not null
			and m.cambio.venta.idVenta = :idVenta
			and m.producto.idProducto = :idProducto
			and (m.tipo = com.inventario.demo.modelo.TipoMovimiento.DEVOLUCION
				or (m.tipo = com.inventario.demo.modelo.TipoMovimiento.ENTRADA
					and m.cambio.tipo = com.inventario.demo.modelo.TipoCambio.DEVOLUCION))
			""")
	java.math.BigDecimal sumadoDevuelto(Integer idVenta, Integer idProducto);

	@Query("""
			select m from Movimiento m
			left join fetch m.cambio
			where m.producto.local.idLocal = :idLocal
			and m.fechaHora >= :desde
			and m.cambio is not null
			order by m.fechaHora desc
			""")
	List<Movimiento> findCambiosRecientes(Integer idLocal, java.time.LocalDateTime desde,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select distinct m.venta from Movimiento m
			where m.venta is not null
			and m.producto.local.idLocal = :idLocal
			and m.venta.fechaHora >= :desde
			order by m.venta.fechaHora desc
			""")
	List<com.inventario.demo.modelo.Venta> findVentasRecientes(Integer idLocal,
			java.time.LocalDateTime desde, org.springframework.data.domain.Pageable pageable);
}
