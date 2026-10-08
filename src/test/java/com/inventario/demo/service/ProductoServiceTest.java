package com.inventario.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.inventario.demo.Dto.PrecioCostoDTO;
import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.interfaces.MovimientoRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.utils.ModeloNotFoundException;
import com.inventario.demo.utils.ReglaNegocioException;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

	@Mock
	private ProductoRepository productoRepository;
	@Mock
	private LocalRepository localRepository;
	@Mock
	private MovimientoRepository movimientoRepository;
	@Mock
	private IMovimientoService movimientoService;

	private ProductoService service;

	@BeforeEach
	void setUp() {
		service = new ProductoService(productoRepository, localRepository, movimientoRepository, movimientoService,
				new ProductoMapper());
	}

	@Test
	void uniformeSinTallaSeRechaza() {
		when(localRepository.findById(1)).thenReturn(Optional.of(local(true)));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.guardar(ficha("8", null)));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(productoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void religiosoConTallaSeRechaza() {
		when(localRepository.findById(1)).thenReturn(Optional.of(local(false)));

		ProductoDTO dto = ficha("Medallas", "M");
		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.guardar(dto));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void religiosoSinCategoriaSeRechaza() {
		when(localRepository.findById(1)).thenReturn(Optional.of(local(false)));

		ProductoDTO dto = ficha(null, null);
		dto.setDetalle("Metal fondo azul");
		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.guardar(dto));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(productoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void actualizarNoTocaElStock() {
		Producto producto = new Producto();
		producto.setIdProducto(5);
		producto.setStock(new BigDecimal("9.00"));
		producto.setLocal(local(true));
		when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
		when(productoRepository.existsByLocalIdLocalAndCodigoIgnoreCaseAndIdProductoNot(1, "CHOMPA-8", 5))
				.thenReturn(false);
		when(productoRepository.save(producto)).thenReturn(producto);

		ProductoDTO dto = ficha("Chompa", "8");
		dto.setIdProducto(5);
		dto.setStock(new BigDecimal("1"));
		ProductoDTO actualizado = service.actualizar(dto);

		assertEquals(0, new BigDecimal("9.00").compareTo(actualizado.getStock()));
	}

	@Test
	void precioCostoSeActualizaSinTocarStock() {
		Producto producto = new Producto();
		producto.setIdProducto(5);
		producto.setStock(new BigDecimal("9.00"));
		producto.setLocal(local(true));
		when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
		when(productoRepository.save(producto)).thenReturn(producto);

		PrecioCostoDTO dto = new PrecioCostoDTO();
		dto.setIdProducto(5);
		dto.setPrecioVenta(new BigDecimal("40"));
		dto.setCostoReferencia(new BigDecimal("30"));
		ProductoDTO actualizado = service.actualizarPrecioCosto(dto);

		assertEquals(0, new BigDecimal("40.00").compareTo(actualizado.getPrecioVenta()));
		assertEquals(0, new BigDecimal("9.00").compareTo(actualizado.getStock()));
	}

	@Test
	void precioNegativoSeRechaza() {
		Producto producto = new Producto();
		producto.setIdProducto(5);
		producto.setLocal(local(true));
		when(productoRepository.findById(5)).thenReturn(Optional.of(producto));

		PrecioCostoDTO dto = new PrecioCostoDTO();
		dto.setIdProducto(5);
		dto.setPrecioVenta(new BigDecimal("-1"));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.actualizarPrecioCosto(dto));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(productoRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void loteConUnaLineaInvalidaNoGuardaNinguna() {
		Producto producto = new Producto();
		producto.setIdProducto(5);
		producto.setLocal(local(true));
		when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
		when(productoRepository.findById(6)).thenReturn(Optional.empty());

		PrecioCostoDTO ok = new PrecioCostoDTO();
		ok.setIdProducto(5);
		ok.setPrecioVenta(new BigDecimal("40"));
		PrecioCostoDTO mala = new PrecioCostoDTO();
		mala.setIdProducto(6);
		mala.setPrecioVenta(new BigDecimal("10"));

		assertThrows(ModeloNotFoundException.class,
				() -> service.actualizarPreciosCostos(java.util.List.of(ok, mala)));
		verify(productoRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void conteoCategoriasPasaLoDelRepositorio() {
		when(productoRepository.contarPorCategoria(1)).thenReturn(java.util.List.of(
				new com.inventario.demo.Dto.CategoriaConteoDTO("Blusas", 8, new java.math.BigDecimal("70"), 1)));

		var conteo = service.contarCategorias(1);

		assertEquals(1, conteo.size());
		assertEquals(1, conteo.get(0).getSinStock());
	}

	private Local local(boolean exigeTalla) {
		Local local = new Local();
		local.setIdLocal(1);
		local.setCodigo(exigeTalla ? "UNIFORMES" : "RELIGIOSOS");
		local.setNombre(local.getCodigo());
		local.setExigeTalla(exigeTalla);
		return local;
	}

	@Test
	void siguienteCodigoProponeElQueSigue() {
		when(productoRepository.findCodigosPorCategoria(1, "BLUSAS"))
				.thenReturn(java.util.List.of("BLU-01", "BLU-08", "BLU-03", "XX-99"));

		var r = service.siguienteCodigo(1, "BLUSAS");

		assertEquals("BLU", r.getPrefijo());
		assertEquals(9, r.getSiguiente());
		assertEquals("BLU-09", r.getCodigo());
	}

	@Test
	void siguienteCodigoSinPreviosDevuelveNulos() {
		when(productoRepository.findCodigosPorCategoria(1, "NUEVA"))
				.thenReturn(java.util.List.of());

		var r = service.siguienteCodigo(1, "NUEVA");

		assertNull(r.getCodigo());
	}

	private ProductoDTO ficha(String tipo, String talla) {
		ProductoDTO dto = new ProductoDTO();
		dto.setIdLocal(1);
		dto.setCodigo("CHOMPA-8");
		dto.setNombre("Chompa");
		dto.setTipo(tipo);
		dto.setTalla(talla);
		dto.setActivo(true);
		return dto;
	}
}
