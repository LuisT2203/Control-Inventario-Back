package com.inventario.demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalDTO {

	private Integer idLocal;
	private String codigo;
	private String nombre;
	private boolean exigeTalla;
}
