package com.inventario.demo.Dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DevolucionDTO {

	@NotNull(message = "La devolución no tiene lineas")
	private List<@Valid LineaDevolucionDTO> lineas;

	private String motivo;

	private Boolean vuelveStock;

	private BigDecimal montoDevuelto;

	@NotNull(message = "La venta de origen es obligatoria")
	private Integer idVenta;
}
