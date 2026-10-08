package com.inventario.demo.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.inventario.demo.modelo.TipoMovimiento;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoDTO {

	private Integer idMovimiento;

	@NotNull(message = "El producto es obligatorio")
	private Integer idProducto;

	private String nombreProducto;
	private String codigoProducto;
	private String tallaProducto;

	@NotNull(message = "El tipo de movimiento es obligatorio")
	private TipoMovimiento tipo;

	@NotNull(message = "La cantidad es obligatoria")
	private BigDecimal cantidad;

	private LocalDateTime fechaHora;
	private BigDecimal saldoResultante;
	private String personaRetira;
	private String destino;
	private BigDecimal costoUnitario;
	private BigDecimal precioUnitario;
	private String grupoCambio;
	private String grupoVenta;
	private String grupoCompra;
	private Integer idVenta;
	private Integer idCompra;
	private String motivo;
}
