package com.inventario.demo.Dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KardexDTO {

	private ProductoDTO producto;
	private List<MovimientoDTO> movimientos;
}
