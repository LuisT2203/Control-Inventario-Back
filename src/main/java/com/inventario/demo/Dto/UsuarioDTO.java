package com.inventario.demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {

	private Integer id;
	private String usuario;
	private boolean estado;
	private String rol;
}
