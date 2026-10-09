package com.inventario.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inventario.demo.Dto.ItemPreviaImportacion;
import com.inventario.demo.Dto.ItemReporteImportacion;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.PreviaImportacionDTO;
import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.Dto.ReporteImportacionDTO;
import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfacesService.IImportacionService;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.interfacesService.IProductoService;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoMovimiento;
import com.inventario.demo.utils.ModeloNotFoundException;
import com.inventario.demo.utils.ReglaNegocioException;

/**
 * Carga de inventario desde los Excel de Rosita. Dos layouts fijos
 * (UNIFORMES y RELIGIOSA) con las mismas columnas del importador historico.
 * Semantica upsert: codigo nuevo crea la ficha completa; codigo existente
 * solo ajusta el stock por diferencia con un movimiento (nunca toca la ficha).
 */
@Service
public class ImportacionInventarioService implements IImportacionService {

	private static final long MAX_BYTES = 10L * 1024 * 1024;
	private static final int FILAS_ENCABEZADO = 7;
	private static final int MAX_FILAS = 2000;
	private static final int MOTIVO_MAX = 120;

	private final ProductoRepository productoRepository;
	private final LocalRepository localRepository;
	private final IProductoService productoService;
	private final IMovimientoService movimientoService;

	public ImportacionInventarioService(ProductoRepository productoRepository, LocalRepository localRepository,
			IProductoService productoService, IMovimientoService movimientoService) {
		this.productoRepository = productoRepository;
		this.localRepository = localRepository;
		this.productoService = productoService;
		this.movimientoService = movimientoService;
	}

	@Override
	@Transactional(readOnly = true)
	public PreviaImportacionDTO previsualizar(Integer idLocal, String origen, MultipartFile archivo) {
		Local local = validarEntrada(idLocal, origen, archivo);
		List<Fila> filas = parsear(origen, archivo);
		Map<String, Producto> existentes = mapaExistentes(idLocal);
		PreviaImportacionDTO previa = new PreviaImportacionDTO();
		previa.setOrigen(origen);
		previa.setArchivo(nombreArchivo(archivo));
		previa.setTotalFilas(filas.size());
		List<ItemPreviaImportacion> items = new ArrayList<>();
		for (Fila fila : filas) {
			items.add(clasificar(fila, existentes.get(fila.codigo)));
		}
		previa.setItems(items);
		previa.setNuevas((int) items.stream().filter(i -> "CREAR".equals(i.getAccion())).count());
		previa.setAjustesEntrada((int) items.stream().filter(i -> "AJUSTE_ENTRADA".equals(i.getAccion())).count());
		previa.setAjustesSalida((int) items.stream().filter(i -> "AJUSTE_SALIDA".equals(i.getAccion())).count());
		previa.setSinCambios((int) items.stream().filter(i -> "SIN_CAMBIOS".equals(i.getAccion())).count());
		previa.setErrores((int) items.stream().filter(i -> "ERROR".equals(i.getAccion())).count());
		return previa;
	}

	@Override
	public ReporteImportacionDTO ejecutar(Integer idLocal, String origen, MultipartFile archivo) {
		Local local = validarEntrada(idLocal, origen, archivo);
		List<Fila> filas = parsear(origen, archivo);
		Map<String, Producto> existentes = mapaExistentes(idLocal);
		String motivo = motivoAjuste(nombreArchivo(archivo));
		ReporteImportacionDTO reporte = new ReporteImportacionDTO();
		reporte.setOrigen(origen);
		reporte.setArchivo(nombreArchivo(archivo));
		List<ItemReporteImportacion> items = new ArrayList<>();
		for (Fila fila : filas) {
			ItemPreviaImportacion vista = clasificar(fila, existentes.get(fila.codigo));
			ItemReporteImportacion item = new ItemReporteImportacion();
			item.setFila(fila.numero);
			item.setCodigo(fila.codigo);
			item.setNombre(fila.nombre);
			try {
				switch (vista.getAccion()) {
				case "CREAR" -> {
					productoService.guardar(fichaNueva(idLocal, fila));
					existentes.put(fila.codigo,
							productoRepository.findByLocalIdLocalAndCodigoIgnoreCase(idLocal, fila.codigo)
									.orElse(null));
					item.setResultado("CREADA");
					item.setMensaje("Ficha creada con stock " + formato(fila.stock) + ".");
				}
				case "AJUSTE_ENTRADA", "AJUSTE_SALIDA" -> {
					Producto producto = existentes.get(fila.codigo);
					movimientoService.registrar(ajuste(producto, vista.getDiferencia(), motivo));
					Producto fresco = productoRepository.findById(producto.getIdProducto()).orElse(producto);
					existentes.put(fila.codigo, fresco);
					item.setResultado("AJUSTADA");
					item.setMensaje(vista.getMensaje());
				}
				case "SIN_CAMBIOS" -> {
					item.setResultado("SIN_CAMBIOS");
					item.setMensaje(vista.getMensaje());
				}
				default -> {
					item.setResultado("ERROR");
					item.setMensaje(vista.getMensaje());
				}
				}
			} catch (ReglaNegocioException e) {
				item.setResultado("ERROR");
				item.setMensaje(e.getMessage());
			} catch (Exception e) {
				item.setResultado("ERROR");
				item.setMensaje("No se pudo procesar la fila.");
			}
			items.add(item);
		}
		reporte.setItems(items);
		reporte.setCreadas((int) items.stream().filter(i -> "CREADA".equals(i.getResultado())).count());
		reporte.setAjustadas((int) items.stream().filter(i -> "AJUSTADA".equals(i.getResultado())).count());
		reporte.setSinCambios((int) items.stream().filter(i -> "SIN_CAMBIOS".equals(i.getResultado())).count());
		reporte.setErrores((int) items.stream().filter(i -> "ERROR".equals(i.getResultado())).count());
		return reporte;
	}

	private Local validarEntrada(Integer idLocal, String origen, MultipartFile archivo) {
		exigirAdmin();
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		Local local = localRepository.findById(idLocal)
				.orElseThrow(() -> new ModeloNotFoundException("Local no encontrado"));
		if (!"UNIFORMES".equals(origen) && !"RELIGIOSA".equals(origen)) {
			throw new ReglaNegocioException("Origen invalido: usa UNIFORMES o RELIGIOSA", HttpStatus.BAD_REQUEST);
		}
		if (("UNIFORMES".equals(origen) && !local.isExigeTalla())
				|| ("RELIGIOSA".equals(origen) && local.isExigeTalla())) {
			throw new ReglaNegocioException("El archivo " + origen + " no corresponde al local " + local.getNombre(),
					HttpStatus.BAD_REQUEST);
		}
		if (archivo == null || archivo.isEmpty()) {
			throw new ReglaNegocioException("Sube un archivo .xlsx", HttpStatus.BAD_REQUEST);
		}
		String nombre = nombreArchivo(archivo).toLowerCase();
		if (!nombre.endsWith(".xlsx")) {
			throw new ReglaNegocioException("El archivo debe ser .xlsx", HttpStatus.BAD_REQUEST);
		}
		if (archivo.getSize() > MAX_BYTES) {
			throw new ReglaNegocioException("El archivo supera los 10 MB", HttpStatus.BAD_REQUEST);
		}
		return local;
	}

	private void exigirAdmin() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		boolean admin = auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		if (!admin) {
			throw new ReglaNegocioException("Solo el administrador puede cargar inventario",
					HttpStatus.FORBIDDEN);
		}
	}

	private Map<String, Producto> mapaExistentes(Integer idLocal) {
		Map<String, Producto> mapa = new LinkedHashMap<>();
		for (Producto p : productoRepository.findByLocalIdLocal(idLocal)) {
			if (p.getCodigo() != null) {
				mapa.putIfAbsent(p.getCodigo().trim().toUpperCase(), p);
			}
		}
		return mapa;
	}

	private List<Fila> parsear(String origen, MultipartFile archivo) {
		List<String> hojas = "UNIFORMES".equals(origen) ? List.of("Inventario")
				: List.of("Medallas", "Vestuario", "Accesorios", "Imágenes");
		List<Fila> filas = new ArrayList<>();
		Map<String, Integer> vistos = new LinkedHashMap<>();
		try (InputStream in = archivo.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
			for (String hoja : hojas) {
				Sheet sheet = wb.getSheet(hoja);
				if (sheet == null) {
					continue;
				}
				int tope = Math.min(sheet.getLastRowNum(), FILAS_ENCABEZADO + MAX_FILAS);
				for (int r = FILAS_ENCABEZADO; r <= tope; r++) {
					Row row = sheet.getRow(r);
					if (row == null) {
						continue;
					}
					Fila fila = leerFila(origen, hoja, r + 1, row);
					if (fila == null) {
						continue;
					}
					if (fila.codigo != null && vistos.containsKey(fila.codigo)) {
						fila.error = "Codigo duplicado en el archivo (fila " + vistos.get(fila.codigo) + ").";
					} else if (fila.codigo != null) {
						vistos.put(fila.codigo, fila.numero);
					}
					filas.add(fila);
					if (filas.size() >= MAX_FILAS) {
						throw new ReglaNegocioException("El archivo supera las " + MAX_FILAS + " filas",
								HttpStatus.BAD_REQUEST);
					}
				}
			}
		} catch (ReglaNegocioException e) {
			throw e;
		} catch (IOException | IllegalArgumentException e) {
			throw new ReglaNegocioException("No se pudo leer el Excel", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			throw new ReglaNegocioException("No se pudo leer el Excel", HttpStatus.BAD_REQUEST);
		}
		if (filas.isEmpty()) {
			throw new ReglaNegocioException("El archivo no trae filas para importar", HttpStatus.BAD_REQUEST);
		}
		return filas;
	}

	private Fila leerFila(String origen, String hoja, int numero, Row row) {
		String codigo = texto(row.getCell(1));
		String nombre = texto(row.getCell(2));
		if ((codigo == null || codigo.isBlank()) && (nombre == null || nombre.isBlank())) {
			return null;
		}
		if (esResumen(codigo, nombre)) {
			return null;
		}
		Fila fila = new Fila();
		fila.numero = numero;
		fila.hoja = hoja;
		fila.codigo = codigo == null ? null : codigo.trim().toUpperCase();
		fila.nombre = nombre == null ? null : nombre.trim();
		if ("UNIFORMES".equals(origen)) {
			fila.tipo = fila.nombre;
			fila.extra = texto(row.getCell(3));
			fila.esTalla = true;
			fila.stock = numero(row.getCell(4), "stock");
			fila.costo = numero(row.getCell(5), "costo");
			fila.precio = numero(row.getCell(6), "precio");
		} else {
			fila.tipo = hoja;
			fila.extra = texto(row.getCell(3));
			fila.esTalla = false;
			fila.stock = numero(row.getCell(4), "stock");
			fila.costo = numero(row.getCell(5), "costo");
			fila.precio = numero(row.getCell(7), "precio");
		}
		fila.error = validarFila(origen, fila);
		return fila;
	}

	private String validarFila(String origen, Fila fila) {
		if (fila.codigo == null || fila.codigo.isBlank()) {
			return "Falta el codigo.";
		}
		if (fila.nombre == null || fila.nombre.isBlank()) {
			return "Falta el nombre.";
		}
		if (fila.stock == null) {
			return "Stock invalido.";
		}
		if (fila.stock.compareTo(BigDecimal.ZERO) < 0) {
			return "El stock no puede ser negativo.";
		}
		if (fila.costo != null && fila.costo.compareTo(BigDecimal.ZERO) < 0) {
			return "El costo no puede ser negativo.";
		}
		if (fila.precio != null && fila.precio.compareTo(BigDecimal.ZERO) < 0) {
			return "El precio no puede ser negativo.";
		}
		if ("UNIFORMES".equals(origen) && (fila.extra == null || fila.extra.isBlank())) {
			return "En uniformes la talla es obligatoria.";
		}
		return null;
	}

	private ItemPreviaImportacion clasificar(Fila fila, Producto existente) {
		ItemPreviaImportacion item = new ItemPreviaImportacion();
		item.setFila(fila.numero);
		item.setCodigo(fila.codigo);
		item.setNombre(fila.nombre);
		if (fila.error != null) {
			item.setAccion("ERROR");
			item.setMensaje(fila.error);
			return item;
		}
		if (existente == null) {
			item.setAccion("CREAR");
			item.setStockNuevo(fila.stock);
			item.setMensaje("Ficha nueva con stock " + formato(fila.stock) + ".");
			return item;
		}
		BigDecimal actual = existente.getStock() == null ? BigDecimal.ZERO : existente.getStock();
		item.setStockActual(actual);
		item.setStockNuevo(fila.stock);
		int cmp = fila.stock.compareTo(actual);
		if (cmp == 0) {
			item.setAccion("SIN_CAMBIOS");
			item.setMensaje("El producto \"" + existente.getNombre() + "\" ya existe y su stock no cambia.");
			return item;
		}
		BigDecimal diferencia = fila.stock.subtract(actual);
		item.setDiferencia(diferencia.abs());
		if (cmp > 0) {
			item.setAccion("AJUSTE_ENTRADA");
		} else {
			item.setAccion("AJUSTE_SALIDA");
		}
		item.setMensaje("El producto \"" + existente.getNombre() + "\" ya existe: solo se actualiza su stock ("
				+ (cmp > 0 ? "+" : "-") + formato(diferencia.abs()) + ").");
		return item;
	}

	private ProductoDTO fichaNueva(Integer idLocal, Fila fila) {
		ProductoDTO dto = new ProductoDTO();
		dto.setIdLocal(idLocal);
		dto.setCodigo(fila.codigo);
		dto.setNombre(fila.nombre);
		dto.setTipo(fila.tipo);
		if (fila.esTalla) {
			dto.setTalla(fila.extra);
		} else {
			dto.setDetalle(fila.extra);
		}
		dto.setStockInicial(fila.stock);
		dto.setCostoReferencia(fila.costo);
		dto.setPrecioVenta(fila.precio);
		dto.setActivo(true);
		return dto;
	}

	private MovimientoDTO ajuste(Producto producto, BigDecimal diferencia, String motivo) {
		MovimientoDTO dto = new MovimientoDTO();
		dto.setIdProducto(producto.getIdProducto());
		dto.setTipo(diferencia.compareTo(BigDecimal.ZERO) > 0 ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA);
		dto.setCantidad(diferencia.abs());
		dto.setCostoUnitario(producto.getCostoReferencia());
		dto.setMotivo(motivo);
		return dto;
	}

	private String motivoAjuste(String archivo) {
		String motivo = "Ajuste Excel: " + archivo;
		return motivo.length() > MOTIVO_MAX ? motivo.substring(0, MOTIVO_MAX) : motivo;
	}

	private String nombreArchivo(MultipartFile archivo) {
		String nombre = archivo.getOriginalFilename();
		if (nombre == null || nombre.isBlank()) {
			return "archivo.xlsx";
		}
		nombre = nombre.replace('\\', '/');
		int corte = nombre.lastIndexOf('/');
		return nombre.substring(corte + 1).trim();
	}

	private boolean esResumen(String codigo, String nombre) {
		String texto = ((codigo == null ? "" : codigo) + " " + (nombre == null ? "" : nombre)).toUpperCase();
		return texto.startsWith("TOTAL") || texto.contains("TOTAL GENERAL");
	}

	private String texto(Cell cell) {
		if (cell == null) {
			return null;
		}
		try {
			CellType tipo = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType()
					: cell.getCellType();
			if (tipo == CellType.STRING) {
				String valor = cell.getStringCellValue();
				return valor == null || valor.isBlank() ? null : valor.trim();
			}
			if (tipo == CellType.NUMERIC) {
				double valor = cell.getNumericCellValue();
				if (valor == Math.rint(valor)) {
					return String.valueOf((long) valor);
				}
				return String.valueOf(valor);
			}
			if (tipo == CellType.BOOLEAN) {
				return String.valueOf(cell.getBooleanCellValue());
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	private BigDecimal numero(Cell cell, String campo) {
		String valor = texto(cell);
		if (valor == null) {
			return "stock".equals(campo) ? BigDecimal.ZERO : null;
		}
		try {
			return new BigDecimal(valor.replace(",", "."));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String formato(BigDecimal valor) {
		if (valor == null) {
			return "0";
		}
		return valor.stripTrailingZeros().toPlainString();
	}

	private static class Fila {
		int numero;
		String hoja;
		String codigo;
		String nombre;
		String tipo;
		String extra;
		boolean esTalla;
		BigDecimal stock;
		BigDecimal costo;
		BigDecimal precio;
		String error;
	}
}
