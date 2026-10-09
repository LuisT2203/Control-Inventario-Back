package com.inventario.demo.Dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreviaImportacionDTO {

	private String origen;
	private String archivo;
	private int totalFilas;
	private int nuevas;
	private int ajustesEntrada;
	private int ajustesSalida;
	private int sinCambios;
	private int errores;
	private List<ItemPreviaImportacion> items;
}
