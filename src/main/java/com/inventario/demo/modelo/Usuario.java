package com.inventario.demo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usu")
	private Integer id;

	@Column(nullable = false, unique = true, length = 100)
	private String usuario;

	@Column(nullable = false, length = 255)
	private String clave;

	@Column(nullable = false)
	private boolean estado = true;

	@ManyToOne(optional = false)
	@JoinColumn(name = "id_rol", nullable = false)
	private Rol rol;
}
