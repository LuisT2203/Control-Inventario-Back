package com.inventario.demo.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.inventario.demo.modelo.TipoCambio;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReciboCambioDTO {

	private Integer idCambio;
	private TipoCambio tipo;
	private LocalDateTime fechaHora;
	private String motivo;
	private BigDecimal diferencia;
	private BigDecimal montoDevuelto;
	private boolean vuelveStock;
	private Integer idVenta;
	private List<MovimientoDTO> lineas;
}
