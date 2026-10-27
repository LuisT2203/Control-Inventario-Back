package com.inventario.demo.Dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumenProductosDTO {

	private long fichas;
	private BigDecimal costoTotal;
	private BigDecimal valorStock;
	private BigDecimal ganancia;
	private long sinCosto;
	private long sinPrecio;
	private long sinGanancia;

	public ResumenProductosDTO(long fichas, BigDecimal costoTotal, BigDecimal valorStock, BigDecimal ganancia,
			long sinCosto, long sinPrecio, long sinGanancia) {
		this.fichas = fichas;
		this.costoTotal = costoTotal;
		this.valorStock = valorStock;
		this.ganancia = ganancia;
		this.sinCosto = sinCosto;
		this.sinPrecio = sinPrecio;
		this.sinGanancia = sinGanancia;
	}
}
