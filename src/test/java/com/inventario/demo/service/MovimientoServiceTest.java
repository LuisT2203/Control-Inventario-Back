package com.inventario.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.LineaCompraDTO;
import com.inventario.demo.Dto.LineaVentaDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;
import com.inventario.demo.interfaces.MovimientoRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.modelo.Movimiento;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoMovimiento;
import com.inventario.demo.utils.ReglaNegocioException;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

	@Mock
	private MovimientoRepository movimientoRepository;
	@Mock
	private ProductoRepository productoRepository;
	@Mock
	private com.inventario.demo.interfaces.VentaRepository ventaRepository;
	@Mock
	private com.inventario.demo.interfaces.CompraRepository compraRepository;
	@Mock
	private ProductoMapper productoMapper;

	private MovimientoService service;
	private Producto chompa;
	private Producto falda;

	@BeforeEach
	void setUp() {
		service = new MovimientoService(movimientoRepository, productoRepository, ventaRepository, compraRepository,
				productoMapper);
		chompa = ficha(1, "4");
		falda = ficha(2, "1");
	}

	@Test
	void entradaSumaStock() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		MovimientoDTO dto = movimiento(1, TipoMovimiento.ENTRADA, "2");
		MovimientoDTO resultado = service.registrar(dto);

		assertEquals(0, new BigDecimal("6.00").compareTo(chompa.getStock()));
		assertEquals(0, new BigDecimal("6.00").compareTo(resultado.getSaldoResultante()));
	}

	@Test
	void salidaRestaStock() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		MovimientoDTO resultado = service.registrar(movimiento(1, TipoMovimiento.SALIDA, "1.50"));

		assertEquals(0, new BigDecimal("2.50").compareTo(resultado.getSaldoResultante()));
	}

	@Test
	void salidaExcesivaNoPersiste() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.registrar(movimiento(1, TipoMovimiento.SALIDA, "9")));

		assertEquals(HttpStatus.CONFLICT, error.getStatus());
		assertEquals(0, new BigDecimal("4").compareTo(chompa.getStock()));
		verify(movimientoRepository, never()).save(any());
		verify(productoRepository, never()).save(any());
	}

	@Test
	void precioNegativoNoPersiste() {
		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> {
			MovimientoDTO dto = movimiento(1, TipoMovimiento.ENTRADA, "1");
			dto.setPrecioUnitario(new BigDecimal("-1"));
			service.registrar(dto);
		});

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(movimientoRepository, never()).save(any());
		verify(productoRepository, never()).save(any());
	}

	@Test
	void cambioDejaDosLineas() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> {
			Movimiento movimiento = inv.getArgument(0);
			movimiento.setIdMovimiento(movimiento.getTipo() == TipoMovimiento.DEVOLUCION ? 10 : 11);
			return movimiento;
		});

		CambioDTO cambio = new CambioDTO();
		cambio.setIdProductoEntra(2);
		cambio.setCantidadEntra(new BigDecimal("1"));
		cambio.setIdProductoSale(1);
		cambio.setCantidadSale(new BigDecimal("2"));

		var lineas = service.registrarCambio(cambio);

		assertEquals(2, lineas.size());
		assertEquals(lineas.get(0).getGrupoCambio(), lineas.get(1).getGrupoCambio());
		assertEquals(0, new BigDecimal("2.00").compareTo(chompa.getStock()));
		assertEquals(0, new BigDecimal("2.00").compareTo(falda.getStock()));
	}

	@Test
	void cambioSinStockNoGuardaNingunaLinea() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));

		CambioDTO cambio = new CambioDTO();
		cambio.setIdProductoEntra(2);
		cambio.setCantidadEntra(new BigDecimal("1"));
		cambio.setIdProductoSale(1);
		cambio.setCantidadSale(new BigDecimal("9"));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.registrarCambio(cambio));

		assertEquals(HttpStatus.CONFLICT, error.getStatus());
		assertEquals(0, new BigDecimal("4").compareTo(chompa.getStock()));
		assertEquals(0, new BigDecimal("1").compareTo(falda.getStock()));
		verify(movimientoRepository, never()).save(any());
	}

	@Test
	void ventaGuardaReciboConNumeroYTotal() {
		chompa.setPrecioVenta(new BigDecimal("35"));
		chompa.setCostoReferencia(new BigDecimal("30"));
		falda.setPrecioVenta(new BigDecimal("5"));
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(ventaRepository.save(any())).thenAnswer(inv -> {
			com.inventario.demo.modelo.Venta v = inv.getArgument(0);
			v.setIdVenta(12);
			return v;
		});

		VentaDTO venta = new VentaDTO();
		venta.setLineas(java.util.List.of(linea(1, "2"), linea(2, "1")));

		var recibo = service.registrarVenta(venta);

		assertEquals(12, recibo.getIdVenta());
		assertEquals(0, new BigDecimal("75.00").compareTo(recibo.getTotal()));
		assertEquals(2, recibo.getLineas().size());
		assertEquals(12, recibo.getLineas().get(0).getIdVenta());
		assertEquals(0, new BigDecimal("35.00").compareTo(recibo.getLineas().get(0).getPrecioUnitario()));
		assertEquals(0, new BigDecimal("2.00").compareTo(chompa.getStock()));
		assertEquals(0, new BigDecimal("0.00").compareTo(falda.getStock()));
	}

	@Test
	void ventaSinPrecioSeBloquea() {
		falda.setPrecioVenta(null);
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));

		VentaDTO venta = new VentaDTO();
		venta.setLineas(java.util.List.of(linea(2, "1")));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.registrarVenta(venta));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(movimientoRepository, never()).save(any());
		verify(ventaRepository, never()).save(any());
	}

	@Test
	void ventaGuardaPrecioNuevoEnFicha() {
		falda.setPrecioVenta(null);
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(ventaRepository.save(any())).thenAnswer(inv -> {
			com.inventario.demo.modelo.Venta v = inv.getArgument(0);
			v.setIdVenta(13);
			return v;
		});

		VentaDTO venta = new VentaDTO();
		LineaVentaDTO l = linea(2, "1");
		l.setPrecioUnitario(new BigDecimal("15"));
		venta.setLineas(java.util.List.of(l));

		var recibo = service.registrarVenta(venta);

		assertEquals(0, new BigDecimal("15.00").compareTo(recibo.getTotal()));
		assertEquals(0, new BigDecimal("15.00").compareTo(falda.getPrecioVenta()));
	}

	@Test
	void ventaIgnoraPrecioDeLineaSiFichaTienePrecio() {
		chompa.setPrecioVenta(new BigDecimal("35"));
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		VentaDTO venta = new VentaDTO();
		LineaVentaDTO l = linea(1, "1");
		l.setPrecioUnitario(new BigDecimal("99"));
		venta.setLineas(java.util.List.of(l));

		var recibo = service.registrarVenta(venta);

		assertEquals(0, new BigDecimal("35.00").compareTo(recibo.getTotal()));
	}

	@Test
	void ventaInexistenteDevuelve404() {
		when(ventaRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(com.inventario.demo.utils.ModeloNotFoundException.class, () -> service.obtenerVenta(99));
	}

	@Test
	void ventaSinStockNoGuardaNingunaLinea() {
		chompa.setPrecioVenta(new BigDecimal("35"));
		falda.setPrecioVenta(new BigDecimal("5"));
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(falda));

		VentaDTO venta = new VentaDTO();
		venta.setLineas(java.util.List.of(linea(1, "1"), linea(2, "9")));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.registrarVenta(venta));

		assertEquals(HttpStatus.CONFLICT, error.getStatus());
		verify(movimientoRepository, never()).save(any());
		verify(productoRepository, never()).save(any());
		verify(ventaRepository, never()).save(any());
	}

	@Test
	void ventaVaciaSeRechaza() {
		VentaDTO venta = new VentaDTO();
		venta.setLineas(java.util.List.of());

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.registrarVenta(venta));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void compraSumaStockConCostoDeLinea() {
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(compraRepository.save(any())).thenAnswer(inv -> {
			com.inventario.demo.modelo.Compra c = inv.getArgument(0);
			c.setIdCompra(3);
			return c;
		});

		CompraDTO compra = new CompraDTO();
		LineaCompraDTO linea = new LineaCompraDTO();
		linea.setIdProducto(1);
		linea.setCantidad(new BigDecimal("5"));
		linea.setCostoUnitario(new BigDecimal("28"));
		compra.setLineas(java.util.List.of(linea));

		var recibo = service.registrarCompra(compra);

		assertEquals(3, recibo.getIdCompra());
		assertEquals(0, new BigDecimal("140.00").compareTo(recibo.getTotal()));
		assertEquals(1, recibo.getLineas().size());
		assertEquals(0, new BigDecimal("9.00").compareTo(chompa.getStock()));
		assertEquals(0, new BigDecimal("28.00").compareTo(recibo.getLineas().get(0).getCostoUnitario()));
		assertEquals(0, new BigDecimal("28.00").compareTo(chompa.getCostoReferencia()));
	}

	@Test
	void ventaGuardaCostoDelMomento() {
		chompa.setPrecioVenta(new BigDecimal("35"));
		chompa.setCostoReferencia(new BigDecimal("30"));
		when(productoRepository.findByIdForUpdate(1)).thenReturn(Optional.of(chompa));
		when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		VentaDTO venta = new VentaDTO();
		venta.setLineas(java.util.List.of(linea(1, "1")));

		var recibo = service.registrarVenta(venta);

		assertEquals(0, new BigDecimal("30.00").compareTo(recibo.getLineas().get(0).getCostoUnitario()));
	}

	@Test
	void compraVaciaSeRechaza() {
		CompraDTO compra = new CompraDTO();
		compra.setLineas(java.util.List.of());

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.registrarCompra(compra));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		verify(movimientoRepository, never()).save(any());
	}

	private LineaVentaDTO linea(int idProducto, String cantidad) {
		LineaVentaDTO linea = new LineaVentaDTO();
		linea.setIdProducto(idProducto);
		linea.setCantidad(new BigDecimal(cantidad));
		return linea;
	}

	private Producto ficha(int id, String stock) {
		Producto producto = new Producto();
		producto.setIdProducto(id);
		producto.setCodigo("P" + id);
		producto.setNombre("Producto " + id);
		producto.setStock(new BigDecimal(stock));
		return producto;
	}

	private MovimientoDTO movimiento(int idProducto, TipoMovimiento tipo, String cantidad) {
		MovimientoDTO dto = new MovimientoDTO();
		dto.setIdProducto(idProducto);
		dto.setTipo(tipo);
		dto.setCantidad(new BigDecimal(cantidad));
		return dto;
	}
}
