package com.inventario.demo.Dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPreviaImportacion {

	private int fila;
	private String codigo;
	private String nombre;
	private String accion;
	private BigDecimal stockActual;
	private BigDecimal stockNuevo;
	private BigDecimal diferencia;
	private String mensaje;
}
