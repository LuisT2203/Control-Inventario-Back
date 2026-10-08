package com.inventario.demo.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cambio")
@Getter
@Setter
@NoArgsConstructor
public class Cambio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cambio")
	private Integer idCambio;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoCambio tipo;

	@Column(length = 120)
	private String motivo;

	@Column(precision = 12, scale = 2)
	private BigDecimal diferencia;

	@Column(name = "monto_devuelto", precision = 12, scale = 2)
	private BigDecimal montoDevuelto;

	@Column(name = "vuelve_stock", nullable = false)
	private boolean vuelveStock = true;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_venta", nullable = false)
	private Venta venta;

	@Column(length = 100)
	private String usuario;
}
