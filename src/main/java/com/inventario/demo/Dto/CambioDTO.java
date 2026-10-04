package com.inventario.demo.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioDTO {

	@NotNull(message = "El producto que vuelve es obligatorio")
	private Integer idProductoEntra;

	@NotNull(message = "La cantidad que vuelve es obligatoria")
	private BigDecimal cantidadEntra;

	@NotNull(message = "El producto que se lleva es obligatorio")
	private Integer idProductoSale;

	@NotNull(message = "La cantidad que se lleva es obligatoria")
	private BigDecimal cantidadSale;

	private String personaRetira;
	private String destino;
	private BigDecimal precioUnitario;
}
