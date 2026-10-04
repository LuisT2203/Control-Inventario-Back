package com.inventario.demo.interfacesService;

import java.util.List;

import com.inventario.demo.Dto.BusquedaProductosDTO;
import com.inventario.demo.Dto.CategoriaConteoDTO;
import com.inventario.demo.Dto.PrecioCostoDTO;
import com.inventario.demo.Dto.ProductoDTO;

public interface IProductoService {

	List<ProductoDTO> listar(Integer idLocal, boolean bajoStock);

	BusquedaProductosDTO buscar(Integer idLocal, String texto, String categoria, boolean bajoStock, boolean sinPrecio, int pagina, int tamano);

	List<String> listarCategorias(Integer idLocal);

	List<CategoriaConteoDTO> contarCategorias(Integer idLocal);

	ProductoDTO listarId(Integer id);

	ProductoDTO guardar(ProductoDTO dto);

	ProductoDTO actualizar(ProductoDTO dto);

	ProductoDTO actualizarPrecioCosto(PrecioCostoDTO dto);

	List<ProductoDTO> actualizarPreciosCostos(List<PrecioCostoDTO> lista);

	void borrar(Integer id);
}
