package com.inventario.demo.interfacesService;

import java.util.List;

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.ReciboCompraDTO;
import com.inventario.demo.Dto.ReciboVentaDTO;
import com.inventario.demo.Dto.KardexDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;

public interface IMovimientoService {

	MovimientoDTO registrar(MovimientoDTO dto);

	List<MovimientoDTO> registrarCambio(CambioDTO dto);

	ReciboVentaDTO registrarVenta(VentaDTO dto);

	ReciboCompraDTO registrarCompra(CompraDTO dto);

	ReciboVentaDTO obtenerVenta(Integer id);

	ReciboCompraDTO obtenerCompra(Integer id);

	List<MovimientoDTO> listarRecientes(Integer idLocal, int dias, Integer idProducto);

	KardexDTO listarKardex(Integer idProducto);
}
