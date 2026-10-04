package com.inventario.demo.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.inventario.demo.modelo.UnidadMedida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoDTO {

	private Integer idProducto;

	@NotNull(message = "El local es obligatorio")
	private Integer idLocal;

	private String codigoLocal;
	private String nombreLocal;

	@NotBlank(message = "El codigo es obligatorio")
	private String codigo;

	@NotBlank(message = "El nombre es obligatorio")
	private String nombre;

	private String tipo;
	private String talla;
	private String detalle;
	private UnidadMedida unidad;
	private BigDecimal stock;
	private BigDecimal stockMinimo;
	private LocalDate fechaVencimiento;
	private BigDecimal costoReferencia;
	private BigDecimal precioVenta;
	private BigDecimal stockInicial;
	private boolean activo = true;
	private boolean bajoStock;
	private boolean vencido;
}
