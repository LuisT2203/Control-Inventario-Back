package com.inventario.demo.interfacesService;

import java.util.List;

import com.inventario.demo.Dto.CambiarClaveDTO;
import com.inventario.demo.Dto.CrearUsuarioDTO;
import com.inventario.demo.Dto.UsuarioDTO;

public interface IUsuarioService {

	List<UsuarioDTO> listar();

	UsuarioDTO crear(CrearUsuarioDTO dto);

	UsuarioDTO cambiarEstado(Integer id, boolean estado);

	UsuarioDTO cambiarClave(Integer id, CambiarClaveDTO dto);
}
