package com.inventario.demo.Dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusquedaProductosDTO {

	private List<ProductoDTO> items;
	private long total;
	private int pagina;
}
