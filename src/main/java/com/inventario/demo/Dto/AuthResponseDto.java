package com.inventario.demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponseDto {

	private String token;
	private String refreshToken;
	private String tipo;
	private String usuario;
}
