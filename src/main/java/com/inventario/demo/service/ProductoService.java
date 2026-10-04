package com.inventario.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventario.demo.Dto.BusquedaProductosDTO;
import com.inventario.demo.Dto.CategoriaConteoDTO;
import com.inventario.demo.Dto.PrecioCostoDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.interfaces.MovimientoRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.interfacesService.IProductoService;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoMovimiento;
import com.inventario.demo.modelo.UnidadMedida;
import com.inventario.demo.utils.ModeloNotFoundException;
import com.inventario.demo.utils.ReglaNegocioException;

@Service
public class ProductoService implements IProductoService {

	private final ProductoRepository productoRepository;
	private final LocalRepository localRepository;
	private final MovimientoRepository movimientoRepository;
	private final IMovimientoService movimientoService;
	private final ProductoMapper productoMapper;

	public ProductoService(ProductoRepository productoRepository, LocalRepository localRepository,
			MovimientoRepository movimientoRepository, IMovimientoService movimientoService,
			ProductoMapper productoMapper) {
		this.productoRepository = productoRepository;
		this.localRepository = localRepository;
		this.movimientoRepository = movimientoRepository;
		this.movimientoService = movimientoService;
		this.productoMapper = productoMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoDTO> listar(Integer idLocal, boolean bajoStock) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		List<Producto> productos = bajoStock ? productoRepository.findBajoStock(idLocal)
				: productoRepository.findByLocalIdLocalAndActivoTrueOrderByNombreAsc(idLocal);
		return productos.stream().map(productoMapper::aDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BusquedaProductosDTO buscar(Integer idLocal, String texto, String categoria, boolean bajoStock,
			boolean sinPrecio, int pagina, int tamano) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		int tope = Math.min(Math.max(tamano, 1), 50);
		org.springframework.data.domain.Page<Producto> page = productoRepository.buscar(idLocal,
				texto == null ? "" : texto.trim(), categoria == null ? "" : categoria, bajoStock, sinPrecio,
				org.springframework.data.domain.PageRequest.of(Math.max(pagina, 0), tope));
		BusquedaProductosDTO respuesta = new BusquedaProductosDTO();
		respuesta.setItems(page.getContent().stream().map(productoMapper::aDto).toList());
		respuesta.setTotal(page.getTotalElements());
		respuesta.setPagina(page.getNumber());
		return respuesta;
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> listarCategorias(Integer idLocal) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return productoRepository.findCategorias(idLocal);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoriaConteoDTO> contarCategorias(Integer idLocal) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return productoRepository.contarPorCategoria(idLocal);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductoDTO listarId(Integer id) {
		return productoMapper.aDto(buscar(id));
	}

	@Override
	@Transactional
	public ProductoDTO guardar(ProductoDTO dto) {
		Local local = buscarLocal(dto.getIdLocal());
		String codigo = normalizarCodigo(dto.getCodigo());
		validarFicha(local, dto.getTipo(), dto.getTalla());
		if (productoRepository.existsByLocalIdLocalAndCodigoIgnoreCase(local.getIdLocal(), codigo)) {
			throw new ReglaNegocioException("Ya existe una ficha con ese codigo en el local", HttpStatus.CONFLICT);
		}
		Producto producto = new Producto();
		producto.setLocal(local);
		producto.setStock(BigDecimal.ZERO.setScale(2));
		copiarDatos(producto, dto, codigo);
		producto = productoRepository.saveAndFlush(producto);
		if (dto.getStockInicial() != null && dto.getStockInicial().compareTo(BigDecimal.ZERO) > 0) {
			MovimientoDTO apertura = new MovimientoDTO();
			apertura.setIdProducto(producto.getIdProducto());
			apertura.setTipo(TipoMovimiento.ENTRADA);
			apertura.setCantidad(dto.getStockInicial());
			apertura.setCostoUnitario(dto.getCostoReferencia());
			apertura.setMotivo("Stock inicial");
			movimientoService.registrar(apertura);
			producto = buscar(producto.getIdProducto());
		}
		return productoMapper.aDto(producto);
	}

	@Override
	@Transactional
	public ProductoDTO actualizar(ProductoDTO dto) {
		Producto producto = buscar(dto.getIdProducto());
		Local local = producto.getLocal();
		if (dto.getIdLocal() != null && !dto.getIdLocal().equals(local.getIdLocal())) {
			local = buscarLocal(dto.getIdLocal());
			producto.setLocal(local);
		}
		String codigo = normalizarCodigo(dto.getCodigo());
		validarFicha(local, dto.getTipo(), dto.getTalla());
		if (productoRepository.existsByLocalIdLocalAndCodigoIgnoreCaseAndIdProductoNot(local.getIdLocal(), codigo,
				producto.getIdProducto())) {
			throw new ReglaNegocioException("Ya existe una ficha con ese codigo en el local", HttpStatus.CONFLICT);
		}
		BigDecimal stock = producto.getStock();
		copiarDatos(producto, dto, codigo);
		producto.setStock(stock);
		return productoMapper.aDto(productoRepository.save(producto));
	}

	@Override
	@Transactional
	public ProductoDTO actualizarPrecioCosto(PrecioCostoDTO dto) {
		Producto producto = buscar(dto.getIdProducto());
		validarPrecioCosto(dto.getPrecioVenta(), dto.getCostoReferencia());
		producto.setPrecioVenta(Cantidades.noNegativo(dto.getPrecioVenta(), "El precio"));
		producto.setCostoReferencia(Cantidades.noNegativo(dto.getCostoReferencia(), "El costo"));
		return productoMapper.aDto(productoRepository.save(producto));
	}

	@Override
	@Transactional
	public List<ProductoDTO> actualizarPreciosCostos(List<PrecioCostoDTO> lista) {
		if (lista == null || lista.isEmpty()) {
			throw new ReglaNegocioException("No hay lineas para guardar", HttpStatus.BAD_REQUEST);
		}
		List<Producto> productos = new ArrayList<>();
		for (PrecioCostoDTO dto : lista) {
			if (dto.getIdProducto() == null) {
				throw new ReglaNegocioException("El producto es obligatorio", HttpStatus.BAD_REQUEST);
			}
			validarPrecioCosto(dto.getPrecioVenta(), dto.getCostoReferencia());
			productos.add(buscar(dto.getIdProducto()));
		}
		List<ProductoDTO> respuesta = new ArrayList<>();
		for (int i = 0; i < lista.size(); i++) {
			Producto producto = productos.get(i);
			PrecioCostoDTO dto = lista.get(i);
			producto.setPrecioVenta(Cantidades.noNegativo(dto.getPrecioVenta(), "El precio"));
			producto.setCostoReferencia(Cantidades.noNegativo(dto.getCostoReferencia(), "El costo"));
			respuesta.add(productoMapper.aDto(productoRepository.save(producto)));
		}
		return respuesta;
	}

	private void validarPrecioCosto(BigDecimal precio, BigDecimal costo) {
		if (precio == null && costo == null) {
			throw new ReglaNegocioException("No hay nada que guardar", HttpStatus.BAD_REQUEST);
		}
		Cantidades.noNegativo(precio, "El precio");
		Cantidades.noNegativo(costo, "El costo");
	}

	@Override
	@Transactional
	public void borrar(Integer id) {
		Producto producto = buscar(id);
		if (movimientoRepository.existsByProductoIdProducto(id)) {
			throw new ReglaNegocioException("No se puede borrar una ficha que ya tiene movimientos", HttpStatus.CONFLICT);
		}
		productoRepository.delete(producto);
	}

	private void copiarDatos(Producto producto, ProductoDTO dto, String codigo) {
		producto.setCodigo(codigo);
		producto.setNombre(dto.getNombre().trim());
		producto.setTipo(blancoANulo(dto.getTipo()));
		producto.setTalla(blancoANulo(dto.getTalla()));
		producto.setDetalle(blancoANulo(dto.getDetalle()));
		producto.setUnidad(dto.getUnidad() == null ? UnidadMedida.UND : dto.getUnidad());
		producto.setStockMinimo(Cantidades.noNegativo(dto.getStockMinimo(), "El stock minimo"));
		producto.setFechaVencimiento(dto.getFechaVencimiento());
		producto.setCostoReferencia(Cantidades.noNegativo(dto.getCostoReferencia(), "El costo"));
		producto.setPrecioVenta(Cantidades.noNegativo(dto.getPrecioVenta(), "El precio"));
		producto.setActivo(dto.isActivo());
	}

	private void validarFicha(Local local, String tipo, String talla) {
		boolean tieneTalla = talla != null && !talla.isBlank();
		boolean tieneTipo = tipo != null && !tipo.isBlank();
		if (!tieneTipo) {
			throw new ReglaNegocioException("La categoria es obligatoria", HttpStatus.BAD_REQUEST);
		}
		if (local.isExigeTalla()) {
			if (!tieneTalla) {
				throw new ReglaNegocioException("En uniformes la talla es obligatoria", HttpStatus.BAD_REQUEST);
			}
			return;
		}
		if (tieneTalla) {
			throw new ReglaNegocioException("Este local no usa talla", HttpStatus.BAD_REQUEST);
		}
	}

	private Producto buscar(Integer id) {
		if (id == null) {
			throw new ReglaNegocioException("El producto es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return productoRepository.findById(id).orElseThrow(() -> new ModeloNotFoundException("Producto no encontrado"));
	}

	private Local buscarLocal(Integer id) {
		if (id == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return localRepository.findById(id).orElseThrow(() -> new ModeloNotFoundException("Local no encontrado"));
	}

	private String normalizarCodigo(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaNegocioException("El codigo es obligatorio", HttpStatus.BAD_REQUEST);
		}
		return codigo.trim().toUpperCase();
	}

	private String blancoANulo(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}
}
