package com.inventario.demo.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.inventario.demo.Dto.ProductoDTO;
import com.inventario.demo.modelo.Producto;

@Component
public class ProductoMapper {

	public ProductoDTO aDto(Producto producto) {
		ProductoDTO dto = new ProductoDTO();
		dto.setIdProducto(producto.getIdProducto());
		dto.setIdLocal(producto.getLocal().getIdLocal());
		dto.setCodigoLocal(producto.getLocal().getCodigo());
		dto.setNombreLocal(producto.getLocal().getNombre());
		dto.setCodigo(producto.getCodigo());
		dto.setNombre(producto.getNombre());
		dto.setTipo(producto.getTipo());
		dto.setTalla(producto.getTalla());
		dto.setDetalle(producto.getDetalle());
		dto.setUnidad(producto.getUnidad());
		dto.setStock(producto.getStock());
		dto.setStockMinimo(producto.getStockMinimo());
		dto.setFechaVencimiento(producto.getFechaVencimiento());
		dto.setCostoReferencia(producto.getCostoReferencia());
		dto.setPrecioVenta(producto.getPrecioVenta());
		dto.setActivo(producto.isActivo());
		dto.setBajoStock(esBajoStock(producto));
		dto.setVencido(producto.getFechaVencimiento() != null && producto.getFechaVencimiento().isBefore(LocalDate.now()));
		return dto;
	}

	private boolean esBajoStock(Producto producto) {
		if (producto.getStock() == null) {
			return true;
		}
		if (producto.getStockMinimo() == null) {
			return producto.getStock().compareTo(java.math.BigDecimal.ZERO) <= 0;
		}
		return producto.getStock().compareTo(producto.getStockMinimo()) <= 0;
	}
}
