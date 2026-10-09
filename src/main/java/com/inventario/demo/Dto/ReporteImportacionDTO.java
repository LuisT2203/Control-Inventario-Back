package com.inventario.demo.Dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteImportacionDTO {

	private String origen;
	private String archivo;
	private int creadas;
	private int ajustadas;
	private int sinCambios;
	private int errores;
	private List<ItemReporteImportacion> items;
}
