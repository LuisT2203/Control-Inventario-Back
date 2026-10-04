package com.inventario.demo.utils;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends RuntimeException {

	private final HttpStatus status;

	public ReglaNegocioException(String mensaje, HttpStatus status) {
		super(mensaje);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
