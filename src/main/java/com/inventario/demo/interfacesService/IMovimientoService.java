package com.inventario.demo.interfacesService;

import java.util.List;

import com.inventario.demo.Dto.CambioDTO;
import com.inventario.demo.Dto.CompraDTO;
import com.inventario.demo.Dto.DevolucionDTO;
import com.inventario.demo.Dto.ReciboCambioDTO;
import com.inventario.demo.Dto.ReciboCompraDTO;
import com.inventario.demo.Dto.ReciboVentaDTO;
import com.inventario.demo.Dto.KardexDTO;
import com.inventario.demo.Dto.MovimientoDTO;
import com.inventario.demo.Dto.VentaDTO;
import com.inventario.demo.Dto.VentaResumenDTO;

public interface IMovimientoService {

	MovimientoDTO registrar(MovimientoDTO dto);

	ReciboCambioDTO registrarCambio(CambioDTO dto);

	ReciboCambioDTO registrarDevolucion(DevolucionDTO dto);

	ReciboCambioDTO obtenerCambio(Integer id);

	List<ReciboCambioDTO> listarCambios(Integer idLocal, int dias);

	List<VentaResumenDTO> listarVentas(Integer idLocal, int dias);

	ReciboVentaDTO registrarVenta(VentaDTO dto);

	ReciboCompraDTO registrarCompra(CompraDTO dto);

	ReciboVentaDTO obtenerVenta(Integer id);

	ReciboCompraDTO obtenerCompra(Integer id);

	List<MovimientoDTO> listarRecientes(Integer idLocal, int dias, Integer idProducto);

	KardexDTO listarKardex(Integer idProducto);
}
