package com.inventario.demo.Dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaConteoDTO {

	private String tipo;
	private long fichas;
	private BigDecimal unidades;
	private long sinStock;

	public CategoriaConteoDTO(String tipo, long fichas, BigDecimal unidades, long sinStock) {
		this.tipo = tipo;
		this.fichas = fichas;
		this.unidades = unidades;
		this.sinStock = sinStock;
	}
}
