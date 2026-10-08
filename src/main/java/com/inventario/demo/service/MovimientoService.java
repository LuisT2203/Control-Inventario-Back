package com.inventario.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

import org.springframework.security.core.context.SecurityContextHolder;

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.DevolucionDTO;
import com.inventario.demo.Dto.LineaCompraDTO;
import com.inventario.demo.Dto.ReciboCambioDTO;
import com.inventario.demo.Dto.ReciboCompraDTO;
import com.inventario.demo.Dto.ReciboVentaDTO;
import com.inventario.demo.Dto.KardexDTO;
import com.inventario.demo.Dto.LineaVentaDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;
import com.inventario.demo.Dto.VentaResumenDTO;
import com.inventario.demo.interfaces.CambioRepository;
import com.inventario.demo.interfaces.CompraRepository;
import com.inventario.demo.interfaces.MovimientoRepository;
import com.inventario.demo.interfaces.ProductoRepository;
import com.inventario.demo.interfaces.VentaRepository;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.modelo.Cambio;
import com.inventario.demo.modelo.Compra;
import com.inventario.demo.modelo.Movimiento;
import com.inventario.demo.modelo.Producto;
import com.inventario.demo.modelo.TipoCambio;
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
	private final CambioRepository cambioRepository;
	private final ProductoMapper productoMapper;

	public MovimientoService(MovimientoRepository movimientoRepository, ProductoRepository productoRepository,
			VentaRepository ventaRepository, CompraRepository compraRepository, CambioRepository cambioRepository,
			ProductoMapper productoMapper) {
		this.movimientoRepository = movimientoRepository;
		this.productoRepository = productoRepository;
		this.ventaRepository = ventaRepository;
		this.compraRepository = compraRepository;
		this.cambioRepository = cambioRepository;
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
	public ReciboCambioDTO registrarCambio(CambioDTO dto) {
		BigDecimal entra = Cantidades.positiva(dto.getCantidadEntra(), "La cantidad que vuelve");
		BigDecimal sale = Cantidades.positiva(dto.getCantidadSale(), "La cantidad que se lleva");
		Cantidades.noNegativo(dto.getPrecioUnitario(), "El precio");
		if (dto.getIdProductoEntra() == null || dto.getIdProductoSale() == null) {
			throw new ReglaNegocioException("El cambio necesita las dos fichas", HttpStatus.BAD_REQUEST);
		}
		Venta venta = exigirVentaOrigen(dto.getIdVenta(), dto.getIdProductoEntra());
		validarTope(dto.getIdVenta(), dto.getIdProductoEntra(), entra);
		bloquearEnOrden(dto.getIdProductoEntra(), dto.getIdProductoSale());
		validarCambio(dto.getIdProductoEntra(), entra, dto.getIdProductoSale(), sale);
		Producto entraProducto = exigirBloqueado(dto.getIdProductoEntra());
		BigDecimal diferencia = null;
		if (entraProducto.getPrecioVenta() != null && dto.getPrecioUnitario() != null) {
			diferencia = dto.getPrecioUnitario().multiply(sale)
					.subtract(entraProducto.getPrecioVenta().multiply(entra))
					.setScale(2, RoundingMode.HALF_UP);
		}
		Cambio cabecera = new Cambio();
		cabecera.setFechaHora(LocalDateTime.now());
		cabecera.setTipo(TipoCambio.CAMBIO);
		cabecera.setMotivo(blancoANulo(dto.getMotivo()) == null ? "Cambio" : blancoANulo(dto.getMotivo()));
		cabecera.setDiferencia(diferencia);
		cabecera.setVuelveStock(true);
		cabecera.setVenta(venta);
		cabecera.setUsuario(usuarioActual());
		cabecera = cambioRepository.save(cabecera);
		String grupo = "CAMBIO-" + cabecera.getIdCambio();
		List<MovimientoDTO> lineas = new ArrayList<>();
		lineas.add(aDto(vincular(aplicarYaBloqueado(dto.getIdProductoEntra(), TipoMovimiento.DEVOLUCION, entra,
				dto.getPersonaRetira(), dto.getDestino(), null, null, cabecera.getMotivo(), grupo), cabecera)));
		lineas.add(aDto(vincular(aplicarYaBloqueado(dto.getIdProductoSale(), TipoMovimiento.SALIDA, sale,
				dto.getPersonaRetira(), dto.getDestino(), null, dto.getPrecioUnitario(), cabecera.getMotivo(),
				grupo), cabecera)));
		return reciboCambio(cabecera, lineas);
	}

	@Override
	@Transactional
	public ReciboCambioDTO registrarDevolucion(DevolucionDTO dto) {
		if (dto.getLineas() == null || dto.getLineas().isEmpty()) {
			throw new ReglaNegocioException("La devolución no tiene lineas", HttpStatus.BAD_REQUEST);
		}
		if (dto.getIdVenta() == null) {
			throw new ReglaNegocioException("La venta de origen es obligatoria", HttpStatus.BAD_REQUEST);
		}
		Venta venta = ventaRepository.findById(dto.getIdVenta())
				.orElseThrow(() -> new ModeloNotFoundException("Venta no encontrada"));
		java.util.Map<Integer, BigDecimal> vendido = vendidoPorProducto(dto.getIdVenta());
		for (com.inventario.demo.Dto.LineaDevolucionDTO linea : dto.getLineas()) {
			if (linea.getIdProducto() == null) {
				throw new ReglaNegocioException("El producto que vuelve es obligatorio", HttpStatus.BAD_REQUEST);
			}
			BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
			if (!vendido.containsKey(linea.getIdProducto())) {
				throw new ReglaNegocioException(
						"La ficha no está en la venta N° " + dto.getIdVenta(), HttpStatus.BAD_REQUEST);
			}
			BigDecimal yaDevuelto = movimientoRepository.sumadoDevuelto(dto.getIdVenta(), linea.getIdProducto());
			BigDecimal pendiente = vendido.get(linea.getIdProducto()).subtract(yaDevuelto);
			if (cantidad.compareTo(pendiente) > 0) {
				throw new ReglaNegocioException("Solo quedan " + pendiente.stripTrailingZeros().toPlainString()
						+ " por devolver en la venta N° " + dto.getIdVenta(), HttpStatus.BAD_REQUEST);
			}
		}
		Cantidades.noNegativo(dto.getMontoDevuelto(), "El monto");
		boolean vuelve = dto.getVuelveStock() == null || dto.getVuelveStock();
		Cambio cabecera = new Cambio();
		cabecera.setFechaHora(LocalDateTime.now());
		cabecera.setTipo(TipoCambio.DEVOLUCION);
		cabecera.setMotivo(blancoANulo(dto.getMotivo()) == null ? "Devolución"
				: "Devolución: " + blancoANulo(dto.getMotivo()));
		cabecera.setMontoDevuelto(dto.getMontoDevuelto());
		cabecera.setVuelveStock(vuelve);
		cabecera.setVenta(venta);
		cabecera.setUsuario(usuarioActual());
		cabecera = cambioRepository.save(cabecera);
		List<MovimientoDTO> lineas = new ArrayList<>();
		if (vuelve) {
			for (com.inventario.demo.Dto.LineaDevolucionDTO linea : dto.getLineas()) {
				BigDecimal cantidad = Cantidades.positiva(linea.getCantidad(), "La cantidad");
				exigirBloqueado(linea.getIdProducto());
				lineas.add(aDto(vincular(aplicarYaBloqueado(linea.getIdProducto(), TipoMovimiento.ENTRADA,
						cantidad, null, null, null, null, cabecera.getMotivo(),
						"DEVOLUCION-" + cabecera.getIdCambio()), cabecera)));
			}
		}
		return reciboCambio(cabecera, lineas);
	}

	@Override
	@Transactional(readOnly = true)
	public ReciboCambioDTO obtenerCambio(Integer id) {
		Cambio cabecera = cambioRepository.findById(id)
				.orElseThrow(() -> new ModeloNotFoundException("Cambio no encontrado"));
		return reciboCambio(cabecera, movimientoRepository.findByCambioIdCambioOrderByIdMovimientoAsc(id)
				.stream().map(this::aDto).toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReciboCambioDTO> listarCambios(Integer idLocal, int dias) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		LocalDateTime desde = LocalDateTime.now().minusDays(Math.max(dias, 1));
		java.util.Map<Integer, List<MovimientoDTO>> porCambio = new java.util.LinkedHashMap<>();
		java.util.Map<Integer, Cambio> cabeceras = new java.util.LinkedHashMap<>();
		for (Movimiento m : movimientoRepository.findCambiosRecientes(idLocal, desde,
				org.springframework.data.domain.PageRequest.of(0, 100))) {
			porCambio.computeIfAbsent(m.getCambio().getIdCambio(), k -> new ArrayList<>()).add(aDto(m));
			cabeceras.putIfAbsent(m.getCambio().getIdCambio(), m.getCambio());
		}
		List<ReciboCambioDTO> respuesta = new ArrayList<>();
		for (java.util.Map.Entry<Integer, List<MovimientoDTO>> e : porCambio.entrySet()) {
			respuesta.add(reciboCambio(cabeceras.get(e.getKey()), e.getValue()));
		}
		return respuesta;
	}

	@Override
	@Transactional(readOnly = true)
	public List<VentaResumenDTO> listarVentas(Integer idLocal, int dias) {
		if (idLocal == null) {
			throw new ReglaNegocioException("El local es obligatorio", HttpStatus.BAD_REQUEST);
		}
		LocalDateTime desde = LocalDateTime.now().minusDays(Math.max(dias, 1));
		return movimientoRepository
				.findVentasRecientes(idLocal, desde, org.springframework.data.domain.PageRequest.of(0, 50))
				.stream().map(v -> {
					VentaResumenDTO r = new VentaResumenDTO();
					r.setIdVenta(v.getIdVenta());
					r.setFechaHora(v.getFechaHora());
					r.setTotal(v.getTotal());
					r.setLineas(movimientoRepository.findByVentaIdVentaOrderByIdMovimientoAsc(v.getIdVenta())
							.stream().map(this::aDto).toList());
					return r;
				}).toList();
	}

	private Venta exigirVentaOrigen(Integer idVenta, Integer idProducto) {
		if (idVenta == null) {
			throw new ReglaNegocioException("La venta de origen es obligatoria", HttpStatus.BAD_REQUEST);
		}
		Venta venta = ventaRepository.findById(idVenta)
				.orElseThrow(() -> new ModeloNotFoundException("Venta no encontrada"));
		if (!vendidoPorProducto(idVenta).containsKey(idProducto)) {
			throw new ReglaNegocioException("La ficha no está en la venta N° " + idVenta, HttpStatus.BAD_REQUEST);
		}
		return venta;
	}

	private java.util.Map<Integer, BigDecimal> vendidoPorProducto(Integer idVenta) {
		java.util.Map<Integer, BigDecimal> vendido = new java.util.HashMap<>();
		for (Movimiento m : movimientoRepository.findByVentaIdVentaOrderByIdMovimientoAsc(idVenta)) {
			vendido.merge(m.getProducto().getIdProducto(), m.getCantidad(), BigDecimal::add);
		}
		return vendido;
	}

	private void validarTope(Integer idVenta, Integer idProducto, BigDecimal cantidad) {
		java.util.Map<Integer, BigDecimal> vendido = vendidoPorProducto(idVenta);
		if (!vendido.containsKey(idProducto)) {
			throw new ReglaNegocioException("La ficha no está en la venta N° " + idVenta, HttpStatus.BAD_REQUEST);
		}
		BigDecimal yaDevuelto = movimientoRepository.sumadoDevuelto(idVenta, idProducto);
		BigDecimal pendiente = vendido.get(idProducto).subtract(yaDevuelto);
		if (cantidad.compareTo(pendiente) > 0) {
			throw new ReglaNegocioException("Solo quedan " + pendiente.stripTrailingZeros().toPlainString()
					+ " por devolver en la venta N° " + idVenta, HttpStatus.BAD_REQUEST);
		}
	}

	private Movimiento vincular(Movimiento movimiento, Cambio cabecera) {
		movimiento.setCambio(cabecera);
		return movimiento;
	}

	private ReciboCambioDTO reciboCambio(Cambio cabecera, List<MovimientoDTO> lineas) {
		ReciboCambioDTO recibo = new ReciboCambioDTO();
		recibo.setIdCambio(cabecera.getIdCambio());
		recibo.setTipo(cabecera.getTipo());
		recibo.setFechaHora(cabecera.getFechaHora());
		recibo.setMotivo(cabecera.getMotivo());
		recibo.setDiferencia(cabecera.getDiferencia());
		recibo.setMontoDevuelto(cabecera.getMontoDevuelto());
		recibo.setVuelveStock(cabecera.isVuelveStock());
		recibo.setIdVenta(cabecera.getVenta() == null ? null : cabecera.getVenta().getIdVenta());
		recibo.setLineas(lineas);
		return recibo;
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
		dto.setTallaProducto(movimiento.getProducto().getTalla());
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
