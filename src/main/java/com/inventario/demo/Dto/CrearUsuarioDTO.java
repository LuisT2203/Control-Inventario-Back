package com.inventario.demo.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearUsuarioDTO {

	@NotBlank(message = "El usuario es obligatorio")
	private String usuario;

	@NotBlank(message = "La clave es obligatoria")
	private String clave;
}
