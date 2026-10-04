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
@Table(name = "movimiento")
@Getter
@Setter
@NoArgsConstructor
public class Movimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Integer idMovimiento;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoMovimiento tipo;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal cantidad;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Column(name = "saldo_resultante", nullable = false, precision = 12, scale = 2)
	private BigDecimal saldoResultante;

	@Column(name = "persona_retira", length = 80)
	private String personaRetira;

	@Column(length = 80)
	private String destino;

	@Column(name = "costo_unitario", precision = 12, scale = 2)
	private BigDecimal costoUnitario;

	@Column(name = "precio_unitario", precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(name = "grupo_cambio", length = 36)
	private String grupoCambio;

	@Column(name = "grupo_venta", length = 40)
	private String grupoVenta;

	@Column(name = "grupo_compra", length = 40)
	private String grupoCompra;

	@ManyToOne
	@JoinColumn(name = "id_venta")
	private Venta venta;

	@ManyToOne
	@JoinColumn(name = "id_compra")
	private Compra compra;

	@Column(length = 120)
	private String motivo;
}
