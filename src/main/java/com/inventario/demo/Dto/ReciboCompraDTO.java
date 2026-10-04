package com.inventario.demo.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReciboCompraDTO {

	private Integer idCompra;
	private LocalDateTime fechaHora;
	private BigDecimal total;
	private List<MovimientoDTO> lineas;
}
