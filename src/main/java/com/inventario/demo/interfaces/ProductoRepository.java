package com.inventario.demo.interfaces;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.inventario.demo.modelo.Producto;

import jakarta.persistence.LockModeType;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

	boolean existsByLocalIdLocalAndCodigoIgnoreCase(Integer idLocal, String codigo);

	boolean existsByLocalIdLocalAndCodigoIgnoreCaseAndIdProductoNot(Integer idLocal, String codigo, Integer idProducto);

	@Query("select p.codigo from Producto p where p.local.idLocal = :idLocal and p.tipo = :categoria")
	List<String> findCodigosPorCategoria(Integer idLocal, String categoria);

	List<Producto> findByLocalIdLocalAndActivoTrueOrderByNombreAsc(Integer idLocal);

	List<Producto> findByLocalIdLocal(Integer idLocal);

	Optional<Producto> findByLocalIdLocalAndCodigoIgnoreCase(Integer idLocal, String codigo);

	@Query("""
			select p from Producto p
			where p.local.idLocal = :idLocal
			and p.activo = true
			and p.stock <= coalesce(p.stockMinimo, 0)
			order by p.nombre
			""")
	List<Producto> findBajoStock(Integer idLocal);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Producto p where p.idProducto = :id")
	Optional<Producto> findByIdForUpdate(Integer id);

	@Query("""
			select p from Producto p
			where p.local.idLocal = :idLocal
			and p.activo = true
			and (:texto is null or :texto = '' or upper(p.codigo) like concat('%', upper(:texto), '%') or upper(p.nombre) like concat('%', upper(:texto), '%'))
			and (:categoria is null or :categoria = '' or p.tipo = :categoria)
			and (:bajoStock = false or p.stock <= coalesce(p.stockMinimo, 0))
			and (:sinPrecio = false or p.precioVenta is null)
			order by p.nombre
			""")
	org.springframework.data.domain.Page<Producto> buscar(Integer idLocal, String texto, String categoria,
			boolean bajoStock, boolean sinPrecio, org.springframework.data.domain.Pageable pageable);

	@Query("select distinct p.tipo from Producto p where p.local.idLocal = :idLocal and p.tipo is not null order by p.tipo")
	List<String> findCategorias(Integer idLocal);

	@Query("""
			select new com.inventario.demo.Dto.CategoriaConteoDTO(p.tipo, count(p), coalesce(sum(p.stock), 0),
				sum(case when p.stock <= 0 then 1 else 0 end))
			from Producto p
			where p.local.idLocal = :idLocal and p.activo = true and p.tipo is not null
			group by p.tipo
			order by p.tipo
			""")
	List<com.inventario.demo.Dto.CategoriaConteoDTO> contarPorCategoria(Integer idLocal);
}
