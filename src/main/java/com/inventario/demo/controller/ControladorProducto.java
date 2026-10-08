package com.inventario.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventario.demo.Dto.PrecioCostoDTO;
import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.interfacesService.IProductoService;
import com.inventario.demo.utils.MensajeResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/ControladorProducto", produces = MediaType.APPLICATION_JSON_VALUE)
public class ControladorProducto {

	private final IProductoService productoService;

	public ControladorProducto(IProductoService productoService) {
		this.productoService = productoService;
	}

	@GetMapping("/listarProductos")
	public ResponseEntity<MensajeResponse> listar(@RequestParam Integer localId,
			@RequestParam(defaultValue = "false") boolean bajoStock) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Productos encontrados")
				.object(productoService.listar(localId, bajoStock)).build());
	}

	@GetMapping("/buscarProductos")
	public ResponseEntity<MensajeResponse> buscar(@RequestParam Integer localId,
			@RequestParam(defaultValue = "") String texto,
			@RequestParam(required = false) String categoria,
			@RequestParam(defaultValue = "false") boolean bajoStock,
			@RequestParam(defaultValue = "false") boolean sinPrecio,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Productos encontrados")
				.object(productoService.buscar(localId, texto, categoria, bajoStock, sinPrecio, pagina, tamano))
				.build());
	}

	@GetMapping("/listarCategorias")
	public ResponseEntity<MensajeResponse> categorias(@RequestParam Integer localId) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Categorias encontradas")
				.object(productoService.listarCategorias(localId)).build());
	}

	@GetMapping("/siguienteCodigo")
	public ResponseEntity<MensajeResponse> siguiente(@RequestParam Integer localId,
			@RequestParam String categoria) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Siguiente código")
				.object(productoService.siguienteCodigo(localId, categoria)).build());
	}

	@GetMapping("/contarCategorias")
	public ResponseEntity<MensajeResponse> conteo(@RequestParam Integer localId) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Conteo por categoria")
				.object(productoService.contarCategorias(localId)).build());
	}

	@GetMapping("/editarProducto/{id}")
	public ResponseEntity<ProductoDTO> editar(@PathVariable Integer id) {
		return ResponseEntity.ok(productoService.listarId(id));
	}

	@PostMapping(value = "/saveProducto", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> guardar(@Valid @RequestBody ProductoDTO dto) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje("Ficha guardada")
				.object(productoService.guardar(dto)).build(), HttpStatus.CREATED);
	}

	@PutMapping(value = "/updateProducto", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> actualizar(@Valid @RequestBody ProductoDTO dto) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Ficha actualizada")
				.object(productoService.actualizar(dto)).build());
	}

	@PutMapping(value = "/updatePrecioCosto", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> precioCosto(@Valid @RequestBody PrecioCostoDTO dto) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Precio y costo actualizados")
				.object(productoService.actualizarPrecioCosto(dto)).build());
	}

	@PutMapping(value = "/updatePreciosCostos", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MensajeResponse> preciosCostos(@Valid @RequestBody List<PrecioCostoDTO> lista) {
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Precios y costos actualizados")
				.object(productoService.actualizarPreciosCostos(lista)).build());
	}

	@DeleteMapping("/eliminarProducto/{id}")
	public ResponseEntity<MensajeResponse> eliminar(@PathVariable Integer id) {
		productoService.borrar(id);
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Ficha eliminada").object(null).build());
	}
}
