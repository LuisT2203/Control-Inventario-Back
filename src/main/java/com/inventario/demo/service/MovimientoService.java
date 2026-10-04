package com.inventario.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

import org.springframework.security.core.context.SecurityContextHolder;

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.LineaCompraDTO;
import com.inventario.demo.Dto.ReciboCompraDTO;
import com.inventario.demo.Dto.ReciboVentaDTO;
import com.inventario.demo.Dto.KardexDTO;
import com.inventario.demo.Dto.LineaVentaDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;
import com.inventario.demo.interfaces.CompraRepository;
import com.inventario.demo.interfaces.MovimientoRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfaces.VentaRepository;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.modelo.Compra;
import com.inventario.demo.modelo.Movimiento;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoMovimiento;
import com.inventario.demo.modelo.Venta;
import com.inventario.demo.utils.ModeloNotFoundException;
import com.inventario.demo.utils.ReglaNegocioException;

@Service
public class MovimientoService implements IMovimientoService {

	private final MovimientoRepository movimientoRepository;
	private final ProductoRepository productoRepository;
	private final VentaRepository ventaRepository;
	private final CompraRepository compraRepository;
	private final ProductoMapper productoMapper;

	public MovimientoService(MovimientoRepository movimientoRepository, ProductoRepository productoRepository,
			VentaRepository ventaRepository, CompraRepository compraRepository, ProductoMapper productoMapper) {
		this.movimientoRepository = movimientoRepository;
		this.productoRepository = productoRepository;
		this.ventaRepository = ventaRepository;
		this.compraRepository = compraRepository;
		this.productoMapper = productoMapper;
	}

	@Override
	@Transactional
	public MovimientoDTO registrar(MovimientoDTO dto) {
		Movimiento movimiento = aplicar(dto.getIdProducto(), dto.getTipo(), dto.getCantidad(), dto.getPersonaRetira(),
				dto.getDestino(), dto.getCostoUnitario(), dto.getPrecioUnitario(), dto.getMotivo(), null);
		return aDto(movimiento);
	}

	@Override
	@Transactional
	public List<MovimientoDTO> registrarCambio(CambioDTO dto) {
		BigDecimal entra = Cantidades.positiva(dto.getCantidadEntra(), "La cantidad que vuelve");
		BigDecimal sale = Cantidades.positiva(dto.getCantidadSale(), "La cantidad que se lleva");
		Cantidades.noNegativo(dto.getPrecioUnitario(), "El precio");
		if (dto.getIdProductoEntra() == null || dto.getIdProductoSale() == null) {
			throw new ReglaNegocioException("El cambio necesita las dos fichas", HttpStatus.BAD_REQUEST);
		}
		bloquearEnOrden(dto.getIdProductoEntra(), dto.getIdProductoSale());
		validarCambio(dto.getIdProductoEntra(), entra, dto.getIdProductoSale(), sale);
		String grupo = UUID.randomUUID().toString();
		List<MovimientoDTO> lineas = new ArrayList<>();
		lineas.add(aDto(aplicarYaBloqueado(dto.getIdProductoEntra(), TipoMovimiento.DEVOLUCION, entra,
				dto.getPersonaRetira(), dto.getDestino(), null, null, "Cambio", grupo)));
		lineas.add(aDto(aplicarYaBloqueado(dto.getIdProductoSale(), TipoMovimiento.SALIDA, sale, dto.getPersonaRetira(),
				dto.getDestino(), null, dto.getPrecioUnitario(), "Cambio", grupo)));
		return lineas;
	}

	@Override
	@Transactional
	public ReciboVentaDTO registrarVenta(VentaDTO dto) {
		if (dto.getLineas() == null || dto.getLineas().isEmpty()) {
			throw new ReglaNegocioException("La venta no tiene lineas", HttpStatus.BAD_REQUEST);
		}
		if (dto.getLineas().stream().anyMatch(l -> l.getIdProducto() == null)) {
			throw new ReglaNegocioException("El producto es obligatorio", HttpStatus.BAD_REQUEST);
		}
		List<Integer> ids = dto.getLineas().stream().map(LineaVentaDTO::getIdProducto).distinct().sorted().toList();
		ids.forEach(this::exigirBloqueado);
		BigDecimal total = BigDecimal.ZERO;
		for (LineaVentaDTO linea : dto.getLineas()) {
			BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
			Producto producto = exigirBloqueado(linea.getIdProducto());
			if (Cantidades.ceroSiNulo(producto.getStock()).compareTo(cantidad) < 0) {
				throw new ReglaNegocioException("No hay stock suficiente de " + producto.getCodigo(),
						HttpStatus.CONFLICT);
			}
			BigDecimal precio = resolverPrecio(producto, linea.getPrecioUnitario());
			if (producto.getPrecioVenta() == null) {
				producto.setPrecioVenta(precio);
				productoRepository.save(producto);
			}
			total = total.add(precio.multiply(cantidad));
		}
		Venta venta = new Venta();
		venta.setFechaHora(LocalDateTime.now());
		venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
		venta.setUsuario(usuarioActual());
		venta = ventaRepository.save(venta);
		List<MovimientoDTO> lineas = new ArrayList<>();
		for (LineaVentaDTO linea : dto.getLineas()) {
			Producto producto = exigirBloqueado(linea.getIdProducto());
			BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
			lineas.add(aDto(aplicarVenta(producto, cantidad, dto.getPersonaRetira(), venta)));
		}
		ReciboVentaDTO recibo = new ReciboVentaDTO();
		recibo.setIdVenta(venta.getIdVenta());
		recibo.setFechaHora(venta.getFechaHora());
		recibo.setTotal(venta.getTotal());
		recibo.setLineas(lineas);
		return recibo;
	}

	@Override
	@Transactional
	public ReciboCompraDTO registrarCompra(CompraDTO dto) {
		if (dto.getLineas() == null || dto.getLineas().isEmpty()) {
			throw new ReglaNegocioException("La compra no tiene lineas", HttpStatus.BAD_REQUEST);
		}
		if (dto.getLineas().stream().anyMatch(l -> l.getIdProducto() == null)) {
			throw new ReglaNegocioException("El producto es obligatorio", HttpStatus.BAD_REQUEST);
		}
		List<Integer> ids = dto.getLineas().stream().map(LineaCompraDTO::getIdProducto).distinct().sorted().toList();
		ids.forEach(this::exigirBloqueado);
		BigDecimal total = BigDecimal.ZERO;
		for (LineaCompraDTO linea : dto.getLineas()) {
			BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
			BigDecimal costo = Cantidades.ceroSiNulo(linea.getCostoUnitario());
			Cantidades.noNegativo(linea.getCostoUnitario(), "El costo");
			total = total.add(costo.multiply(cantidad));
		}
		Compra compra = new Compra();
		compra.setFechaHora(LocalDateTime.now());
		compra.setTotal(total.setScale(2, RoundingMode.HALF_UP));
		String motivo = blancoANulo(dto.getMotivo());
		compra.setMotivo(motivo);
		compra = compraRepository.save(compra);
		compra.setMotivo(motivo == null ? "Compra N° " + compra.getIdCompra() : motivo);
		List<MovimientoDTO> lineas = new ArrayList<>();
		for (LineaCompraDTO linea : dto.getLineas()) {
			Producto producto = exigirBloqueado(linea.getIdProducto());
			BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
			lineas.add(aDto(aplicarCompra(producto, cantidad, linea.getCostoUnitario(), compra)));
		}
		ReciboCompraDTO recibo = new ReciboCompraDTO();
		recibo.setIdCompra(compra.getIdCompra());
		recibo.setFechaHora(compra.getFechaHora());
		recibo.setTotal(compra.getTotal());
		recibo.setLineas(lineas);
		return recibo;
	}

	@Override
	@Transactional(readOnly = true)
	public List<MovimientoDTO> listarRecientes(Integer idLocal, int dias, Integer idProducto) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		LocalDateTime desde = LocalDateTime.now().minusDays(Math.max(dias, 1));
		return movimientoRepository
				.findRecientes(idLocal, desde, idProducto,
						org.springframework.data.domain.PageRequest.of(0, 100)).stream()
				.map(this::aDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ReciboVentaDTO obtenerVenta(Integer id) {
		Venta venta = ventaRepository.findById(id)
				.orElseThrow(() -> new ModeloNotFoundException("Venta no encontrada"));
		ReciboVentaDTO recibo = new ReciboVentaDTO();
		recibo.setIdVenta(venta.getIdVenta());
		recibo.setFechaHora(venta.getFechaHora());
		recibo.setTotal(venta.getTotal());
		recibo.setLineas(movimientoRepository.findByVentaIdVentaOrderByIdMovimientoAsc(id).stream()
				.map(this::aDto).toList());
		return recibo;
	}

	@Override
	@Transactional(readOnly = true)
	public ReciboCompraDTO obtenerCompra(Integer id) {
		Compra compra = compraRepository.findById(id)
				.orElseThrow(() -> new ModeloNotFoundException("Compra no encontrada"));
		ReciboCompraDTO recibo = new ReciboCompraDTO();
		recibo.setIdCompra(compra.getIdCompra());
		recibo.setFechaHora(compra.getFechaHora());
		recibo.setTotal(compra.getTotal());
		recibo.setLineas(movimientoRepository.findByCompraIdCompraOrderByIdMovimientoAsc(id).stream()
				.map(this::aDto).toList());
		return recibo;
	}

	@Override
	@Transactional(readOnly = true)
	public KardexDTO listarKardex(Integer idProducto) {
		Producto producto = productoRepository.findById(idProducto)
				.orElseThrow(() -> new ModeloNotFoundException("Producto no encontrado"));
		KardexDTO kardex = new KardexDTO();
		kardex.setProducto(productoMapper.aDto(producto));
		kardex.setMovimientos(movimientoRepository
				.findByProductoIdProductoOrderByFechaHoraAscIdMovimientoAsc(idProducto).stream().map(this::aDto)
				.toList());
		return kardex;
	}

	private void bloquearEnOrden(Integer primero, Integer segundo) {
		if (primero.equals(segundo)) {
			exigirBloqueado(primero);
			return;
		}
		Integer menor = primero < segundo ? primero : segundo;
		Integer mayor = primero < segundo ? segundo : primero;
		exigirBloqueado(menor);
		exigirBloqueado(mayor);
	}

	private Producto exigirBloqueado(Integer id) {
		return productoRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ModeloNotFoundException("Producto no encontrado"));
	}

	private void validarCambio(Integer idEntra, BigDecimal entra, Integer idSale, BigDecimal sale) {
		Producto entraProducto = exigirBloqueado(idEntra);
		if (idEntra.equals(idSale)) {
			BigDecimal neto = Cantidades.ceroSiNulo(entraProducto.getStock()).add(entra).subtract(sale);
			if (neto.compareTo(BigDecimal.ZERO) < 0) {
				throw new ReglaNegocioException("No hay stock suficiente para el cambio", HttpStatus.CONFLICT);
			}
			return;
		}
		Producto saleProducto = exigirBloqueado(idSale);
		if (Cantidades.ceroSiNulo(saleProducto.getStock()).compareTo(sale) < 0) {
			throw new ReglaNegocioException("No hay stock suficiente para el cambio", HttpStatus.CONFLICT);
		}
	}

	private Movimiento aplicar(Integer idProducto, TipoMovimiento tipo, BigDecimal cantidad, String persona,
			String destino, BigDecimal costo, BigDecimal precio, String motivo, String grupo) {
		if (idProducto == null) {
			throw new ReglaNegocioException("El producto es obligatorio", HttpStatus.BAD_REQUEST);
		}
		if (tipo == null) {
			throw new ReglaNegocioException("El tipo de movimiento es obligatorio", HttpStatus.BAD_REQUEST);
		}
		Cantidades.positiva(cantidad, "La cantidad");
		Cantidades.noNegativo(costo, "El costo");
		Cantidades.noNegativo(precio, "El precio");
		exigirBloqueado(idProducto);
		return aplicarYaBloqueado(idProducto, tipo, cantidad, persona, destino, costo, precio, motivo, grupo);
	}

	private Movimiento aplicarYaBloqueado(Integer idProducto, TipoMovimiento tipo, BigDecimal cantidad, String persona,
			String destino, BigDecimal costo, BigDecimal precio, String motivo, String grupo) {
		if (tipo == null) {
			throw new ReglaNegocioException("El tipo de movimiento es obligatorio", HttpStatus.BAD_REQUEST);
		}
		BigDecimal cantidadOk = Cantidades.positiva(cantidad, "La cantidad");
		BigDecimal costoOk = Cantidades.noNegativo(costo, "El costo");
		BigDecimal precioOk = Cantidades.noNegativo(precio, "El precio");
		Producto producto = exigirBloqueado(idProducto);
		BigDecimal saldo = calcularSaldo(producto, tipo, cantidadOk);
		producto.setStock(saldo);
		productoRepository.save(producto);
		Movimiento movimiento = new Movimiento();
		movimiento.setProducto(producto);
		movimiento.setTipo(tipo);
		movimiento.setCantidad(cantidadOk);
		movimiento.setFechaHora(LocalDateTime.now());
		movimiento.setSaldoResultante(saldo);
		movimiento.setPersonaRetira(blancoANulo(persona));
		movimiento.setDestino(blancoANulo(destino));
		movimiento.setCostoUnitario(costoOk);
		movimiento.setPrecioUnitario(precioOk);
		movimiento.setMotivo(blancoANulo(motivo));
		movimiento.setGrupoCambio(grupo);
		return movimientoRepository.save(movimiento);
	}

	private BigDecimal resolverPrecio(Producto producto, BigDecimal precioLinea) {
		if (producto.getPrecioVenta() != null) {
			return producto.getPrecioVenta();
		}
		BigDecimal precio = Cantidades.noNegativo(precioLinea, "El precio");
		if (precio == null) {
			throw new ReglaNegocioException(
					"El producto " + producto.getCodigo() + " no tiene precio: escribilo en la venta",
					HttpStatus.BAD_REQUEST);
		}
		return precio;
	}

	private Movimiento aplicarVenta(Producto producto, BigDecimal cantidad, String persona, Venta venta) {
		BigDecimal saldo = calcularSaldo(producto, TipoMovimiento.SALIDA, cantidad);
		producto.setStock(saldo);
		productoRepository.save(producto);
		Movimiento movimiento = new Movimiento();
		movimiento.setProducto(producto);
		movimiento.setTipo(TipoMovimiento.SALIDA);
		movimiento.setCantidad(cantidad);
		movimiento.setFechaHora(LocalDateTime.now());
		movimiento.setSaldoResultante(saldo);
		movimiento.setPersonaRetira(blancoANulo(persona));
		movimiento.setPrecioUnitario(Cantidades.noNegativo(producto.getPrecioVenta(), "El precio"));
		movimiento.setCostoUnitario(Cantidades.noNegativo(producto.getCostoReferencia(), "El costo"));
		movimiento.setMotivo("Venta N° " + venta.getIdVenta());
		movimiento.setVenta(venta);
		return movimientoRepository.save(movimiento);
	}

	private Movimiento aplicarCompra(Producto producto, BigDecimal cantidad, BigDecimal costo, Compra compra) {
		BigDecimal saldo = calcularSaldo(producto, TipoMovimiento.ENTRADA, cantidad);
		producto.setStock(saldo);
		BigDecimal costoOk = Cantidades.noNegativo(costo, "El costo");
		if (costoOk != null) {
			producto.setCostoReferencia(costoOk);
		}
		productoRepository.save(producto);
		Movimiento movimiento = new Movimiento();
		movimiento.setProducto(producto);
		movimiento.setTipo(TipoMovimiento.ENTRADA);
		movimiento.setCantidad(cantidad);
		movimiento.setFechaHora(LocalDateTime.now());
		movimiento.setSaldoResultante(saldo);
		movimiento.setCostoUnitario(Cantidades.noNegativo(costo, "El costo"));
		movimiento.setPrecioUnitario(Cantidades.noNegativo(producto.getPrecioVenta(), "El precio"));
		movimiento.setMotivo(compra.getMotivo());
		movimiento.setCompra(compra);
		return movimientoRepository.save(movimiento);
	}

	private String usuarioActual() {
		try {
			String nombre = org.springframework.security.core.context.SecurityContextHolder.getContext()
					.getAuthentication().getName();
			return nombre == null ? "mostrador" : nombre;
		} catch (Exception e) {
			return "mostrador";
		}
	}

	private BigDecimal calcularSaldo(Producto producto, TipoMovimiento tipo, BigDecimal cantidad) {
		BigDecimal actual = Cantidades.ceroSiNulo(producto.getStock());
		if (tipo == TipoMovimiento.SALIDA) {
			if (actual.compareTo(cantidad) < 0) {
				throw new ReglaNegocioException("No hay stock suficiente", HttpStatus.CONFLICT);
			}
			return actual.subtract(cantidad);
		}
		return actual.add(cantidad);
	}

	private MovimientoDTO aDto(Movimiento movimiento) {
		MovimientoDTO dto = new MovimientoDTO();
		dto.setIdMovimiento(movimiento.getIdMovimiento());
		dto.setIdProducto(movimiento.getProducto().getIdProducto());
		dto.setNombreProducto(movimiento.getProducto().getNombre());
		dto.setCodigoProducto(movimiento.getProducto().getCodigo());
		dto.setTipo(movimiento.getTipo());
		dto.setCantidad(movimiento.getCantidad());
		dto.setFechaHora(movimiento.getFechaHora());
		dto.setSaldoResultante(movimiento.getSaldoResultante());
		dto.setPersonaRetira(movimiento.getPersonaRetira());
		dto.setDestino(movimiento.getDestino());
		dto.setCostoUnitario(movimiento.getCostoUnitario());
		dto.setPrecioUnitario(movimiento.getPrecioUnitario());
		dto.setGrupoCambio(movimiento.getGrupoCambio());
		dto.setGrupoVenta(movimiento.getGrupoVenta());
		dto.setGrupoCompra(movimiento.getGrupoCompra());
		dto.setIdVenta(movimiento.getVenta() == null ? null : movimiento.getVenta().getIdVenta());
		dto.setIdCompra(movimiento.getCompra() == null ? null : movimiento.getCompra().getIdCompra());
		dto.setMotivo(movimiento.getMotivo());
		return dto;
	}

	private String blancoANulo(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}
}
