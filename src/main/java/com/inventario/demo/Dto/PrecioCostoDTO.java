package com.inventario.demo.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrecioCostoDTO {

	@NotNull(message = "El producto es obligatorio")
	private Integer idProducto;

	private BigDecimal precioVenta;
	private BigDecimal costoReferencia;
}
