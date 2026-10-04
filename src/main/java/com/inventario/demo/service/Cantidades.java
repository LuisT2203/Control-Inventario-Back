package com.inventario.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;

import com.inventario.demo.utils.ReglaNegocioException;

public final class Cantidades {

	private Cantidades() {
	}

	public static BigDecimal positiva(BigDecimal valor, String campo) {
		if (valor == null) {
			throw new ReglaNegocioException(campo + " es obligatorio", HttpStatus.BAD_REQUEST);
		}
		BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
		if (normalizado.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ReglaNegocioException(campo + " debe ser mayor que cero", HttpStatus.BAD_REQUEST);
		}
		return normalizado;
	}

	public static BigDecimal noNegativo(BigDecimal valor, String campo) {
		if (valor == null) {
			return null;
		}
		BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
		if (normalizado.compareTo(BigDecimal.ZERO) < 0) {
			throw new ReglaNegocioException(campo + " no puede ser negativo", HttpStatus.BAD_REQUEST);
		}
		return normalizado;
	}

	public static BigDecimal ceroSiNulo(BigDecimal valor) {
		return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : valor.setScale(2, RoundingMode.HALF_UP);
	}
}
