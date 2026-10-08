package com.inventario.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.DevolucionDTO;
import com.inventario.demo.Dto.ReciboCambioDTO;
import com.inventario.demo.Dto.ReciboCompraDTO;
import com.inventario.demo.Dto.ReciboVentaDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;
import com.inventario.demo.interfacesService.IMovimientoService;
import com.inventario.demo.utils.MensajeResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/ControladorMovimiento", produces = MediaType.APPLICATION_JSON_VALUE)
public class ControladorMovimiento {

	private final IMovimientoService movimientoService;

	public ControladorMovimiento(IMovimientoService movimientoService) {
		this.movimientoService = movimientoService;
	}

	@PostMapping(value = "/saveMovimiento", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> guardar(@Valid @RequestBody MovimientoDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Movimiento registrado")
				.object(movimientoService.registrar(dto)).build(), HttpStatus.CREATED);
	}

	@PostMapping(value = "/saveCambio", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> cambio(@Valid @RequestBody CambioDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Cambio registrado")
				.object(movimientoService.registrarCambio(dto)).build(), HttpStatus.CREATED);
	}

	@PostMapping(value = "/saveDevolucion", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> devolucion(@Valid @RequestBody DevolucionDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Devolución registrada")
				.object(movimientoService.registrarDevolucion(dto)).build(), HttpStatus.CREATED);
	}

	@GetMapping("/cambio/{id}")
	public ResponseEntity<ReciboCambioDTO> verCambio(@PathVariable Integer id) {
		return ResponseEntity.ok(movimientoService.obtenerCambio(id));
	}

	@GetMapping("/listarCambios")
	public ResponseEntity<MensajeResponse> cambios(@RequestParam Integer localId,
			@RequestParam(defaultValue = "30") int dias) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Cambios y devoluciones")
				.object(movimientoService.listarCambios(localId, dias)).build());
	}

	@GetMapping("/listarVentas")
	public ResponseEntity<MensajeResponse> ventas(@RequestParam Integer localId,
			@RequestParam(defaultValue = "30") int dias) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Ventas recientes")
				.object(movimientoService.listarVentas(localId, dias)).build());
	}

	@PostMapping(value = "/saveVenta", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> venta(@Valid @RequestBody VentaDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Venta registrada")
				.object(movimientoService.registrarVenta(dto)).build(), HttpStatus.CREATED);
	}

	@PostMapping(value = "/saveCompra", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> compra(@Valid @RequestBody CompraDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Compra registrada")
				.object(movimientoService.registrarCompra(dto)).build(), HttpStatus.CREATED);
	}

	@GetMapping("/venta/{id}")
	public ResponseEntity<ReciboVentaDTO> verVenta(@PathVariable Integer id) {
		return ResponseEntity.ok(movimientoService.obtenerVenta(id));
	}

	@GetMapping("/compra/{id}")
	public ResponseEntity<ReciboCompraDTO> verCompra(@PathVariable Integer id) {
		return ResponseEntity.ok(movimientoService.obtenerCompra(id));
	}

	@GetMapping("/listarRecientes")
	public ResponseEntity<MensajeResponse> recientes(@RequestParam Integer localId,
			@RequestParam(defaultValue = "1") int dias,
			@RequestParam(required = false) Integer idProducto) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Movimientos recientes")
				.object(movimientoService.listarRecientes(localId, dias, idProducto)).build());
	}

	@GetMapping("/listarKardex/{idProducto}")
	public ResponseEntity<MensajeResponse> kardex(@PathVariable Integer idProducto) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Kardex encontrado")
				.object(movimientoService.listarKardex(idProducto)).build());
	}
}
