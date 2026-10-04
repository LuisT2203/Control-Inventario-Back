package com.inventario.demo.Dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompraDTO {

	@NotEmpty(message = "La compra no tiene lineas")
	private List<@Valid LineaCompraDTO> lineas;

	private String motivo;
}
