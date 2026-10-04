package com.inventario.demo.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LineaCompraDTO {

	@NotNull(message = "El producto es obligatorio")
	private Integer idProducto;

	@NotNull(message = "La cantidad es obligatoria")
	private BigDecimal cantidad;

	private BigDecimal costoUnitario;
}
