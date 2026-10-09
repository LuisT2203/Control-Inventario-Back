package com.inventario.demo.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarClaveDTO {

	@NotBlank(message = "La clave es obligatoria")
	private String clave;
}
