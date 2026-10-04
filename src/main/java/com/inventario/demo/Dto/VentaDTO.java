package com.inventario.demo.Dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaDTO {

	@NotEmpty(message = "La venta no tiene lineas")
	private List<@Valid LineaVentaDTO> lineas;

	private String personaRetira;
	private String motivo;
}
