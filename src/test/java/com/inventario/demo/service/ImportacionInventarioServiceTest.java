package com.inventario.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.PreviaImportacionDTO;
import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.Dto.ReporteImportacionDTO;
import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.interfacesService.IProductoService;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoMovimiento;
import com.inventario.demo.utils.ReglaNegocioException;

@ExtendWith(MockitoExtension.class)
class ImportacionInventarioServiceTest {

	@Mock
	private ProductoRepository productoRepository;
	@Mock
	private LocalRepository localRepository;
	@Mock
	private IProductoService productoService;
	@Mock
	private IMovimientoService movimientoService;

	private ImportacionInventarioService service;

	@BeforeEach
	void setUp() {
		service = new ImportacionInventarioService(productoRepository, localRepository, productoService,
				movimientoService);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("rosita", "x",
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void previaClasificaCrearAjusteSinCambiosYErrores() throws Exception {
		when(localRepository.findById(1)).thenReturn(Optional.of(local()));
		when(productoRepository.findByLocalIdLocal(1))
				.thenReturn(List.of(producto(10, "IM-01", "Chompa", 10), producto(11, "IM-04", "Buzo", 7)));

		PreviaImportacionDTO previa = service.previsualizar(1, "UNIFORMES", archivo(
				fila("IM-02", "Falda", "M", 5, 20, 30),
				fila("IM-01", "Chompa", "M", 12, 20, 30),
				fila("IM-04", "Buzo", "L", 7, 20, 30),
				fila(null, "Sin codigo", "M", 1, 20, 30),
				fila("IM-02", "Falda", "M", 5, 20, 30)));

		assertEquals(5, previa.getTotalFilas());
		assertEquals(1, previa.getNuevas());
		assertEquals(1, previa.getAjustesEntrada());
		assertEquals(0, previa.getAjustesSalida());
		assertEquals(1, previa.getSinCambios());
		assertEquals(2, previa.getErrores());
		var ajuste = previa.getItems().stream().filter(i -> "IM-01".equals(i.getCodigo())).findFirst().orElseThrow();
		assertEquals("AJUSTE_ENTRADA", ajuste.getAccion());
		assertEquals(0, ajuste.getDiferencia().compareTo(new BigDecimal("2")));
		assertTrue(ajuste.getMensaje().contains("ya existe"));
	}

	@Test
	void ejecutarCreaYAjustaPorDiferencia() throws Exception {
		when(localRepository.findById(1)).thenReturn(Optional.of(local()));
		Producto existente = producto(10, "IM-01", "Chompa", 10);
		when(productoRepository.findByLocalIdLocal(1)).thenReturn(List.of(existente));
		when(productoRepository.findByLocalIdLocalAndCodigoIgnoreCase(1, "IM-02"))
				.thenReturn(Optional.of(producto(99, "IM-02", "Falda", 5)));
		when(productoRepository.findById(10)).thenReturn(Optional.of(existente));
		when(productoService.guardar(any(ProductoDTO.class))).thenReturn(new ProductoDTO());
		when(movimientoService.registrar(any(MovimientoDTO.class))).thenReturn(new MovimientoDTO());

		ReporteImportacionDTO reporte = service.ejecutar(1, "UNIFORMES",
				archivo(fila("IM-02", "Falda", "M", 5, 20, 30), fila("IM-01", "Chompa", "M", 13, 20, 30)));

		assertEquals(1, reporte.getCreadas());
		assertEquals(1, reporte.getAjustadas());
		assertEquals(0, reporte.getErrores());
		ArgumentCaptor<MovimientoDTO> captor = ArgumentCaptor.forClass(MovimientoDTO.class);
		verify(movimientoService).registrar(captor.capture());
		assertEquals(TipoMovimiento.ENTRADA, captor.getValue().getTipo());
		assertEquals(0, captor.getValue().getCantidad().compareTo(new BigDecimal("3")));
		assertTrue(captor.getValue().getMotivo().startsWith("Ajuste Excel: "));
	}

	@Test
	void ejecutarSalidaSinStockEsErrorYElRestoSigue() throws Exception {
		when(localRepository.findById(1)).thenReturn(Optional.of(local()));
		Producto existente = producto(10, "IM-01", "Chompa", 3);
		when(productoRepository.findByLocalIdLocal(1)).thenReturn(List.of(existente));
		when(movimientoService.registrar(any(MovimientoDTO.class)))
				.thenThrow(new ReglaNegocioException("No hay stock suficiente", HttpStatus.CONFLICT));
		when(productoService.guardar(any(ProductoDTO.class))).thenReturn(new ProductoDTO());

		ReporteImportacionDTO reporte = service.ejecutar(1, "UNIFORMES",
				archivo(fila("IM-01", "Chompa", "M", 0, 20, 30), fila("IM-09", "Falda", "M", 4, 20, 30)));

		assertEquals(1, reporte.getCreadas());
		assertEquals(0, reporte.getAjustadas());
		assertEquals(1, reporte.getErrores());
		assertEquals("ERROR", reporte.getItems().get(0).getResultado());
	}

	@Test
	void sinRolAdminSeRechaza() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y",
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));

		ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
				() -> service.previsualizar(1, "UNIFORMES", archivo(fila("IM-02", "Falda", "M", 5, 20, 30))));

		assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
	}

	private Local local() {
		Local local = new Local();
		local.setIdLocal(1);
		local.setCodigo("UNIFORMES");
		local.setNombre("Uniformes");
		local.setExigeTalla(true);
		return local;
	}

	private Producto producto(int id, String codigo, String nombre, int stock) {
		Producto producto = new Producto();
		producto.setIdProducto(id);
		producto.setCodigo(codigo);
		producto.setNombre(nombre);
		producto.setStock(new BigDecimal(stock));
		producto.setCostoReferencia(new BigDecimal("20"));
		return producto;
	}

	private Object[] fila(String codigo, String nombre, String talla, int stock, int costo, int precio) {
		return new Object[] { codigo, nombre, talla, stock, costo, precio };
	}

	private MultipartFile archivo(Object[]... filas) throws Exception {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("Inventario");
			int r = 7;
			for (Object[] f : filas) {
				Row row = sheet.createRow(r++);
				if (f[0] != null) {
					row.createCell(1).setCellValue((String) f[0]);
				}
				if (f[1] != null) {
					row.createCell(2).setCellValue((String) f[1]);
				}
				if (f[2] != null) {
					row.createCell(3).setCellValue((String) f[2]);
				}
				row.createCell(4).setCellValue(((Number) f[3]).doubleValue());
				row.createCell(5).setCellValue(((Number) f[4]).doubleValue());
				row.createCell(6).setCellValue(((Number) f[5]).doubleValue());
			}
			wb.write(out);
			return new MockMultipartFile("archivo", "uniformes.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
		}
	}
}
