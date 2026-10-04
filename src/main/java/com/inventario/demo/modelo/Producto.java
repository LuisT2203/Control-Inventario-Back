package com.inventario.demo.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto", uniqueConstraints = @UniqueConstraint(columnNames = { "id_local", "codigo" }))
@Getter
@Setter
@NoArgsConstructor
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Integer idProducto;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_local", nullable = false)
	private Local local;

	@Column(nullable = false, length = 40)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(length = 40)
	private String tipo;

	@Column(length = 20)
	private String talla;

	@Column(length = 120)
	private String detalle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private UnidadMedida unidad = UnidadMedida.UND;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal stock = BigDecimal.ZERO;

	@Column(name = "stock_minimo", precision = 12, scale = 2)
	private BigDecimal stockMinimo;

	@Column(name = "fecha_vencimiento")
	private LocalDate fechaVencimiento;

	@Column(name = "costo_referencia", precision = 12, scale = 2)
	private BigDecimal costoReferencia;

	@Column(name = "precio_venta", precision = 12, scale = 2)
	private BigDecimal precioVenta;

	@Column(nullable = false)
	private boolean activo = true;
}
