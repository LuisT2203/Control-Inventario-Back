package com.inventario.demo.interfacesService;

import org.springframework.web.multipart.MultipartFile;

import com.inventario.demo.Dto.PreviaImportacionDTO;
import com.inventario.demo.Dto.ReporteImportacionDTO;

public interface IImportacionService {

	PreviaImportacionDTO previsualizar(Integer idLocal, String origen, MultipartFile archivo);

	ReporteImportacionDTO ejecutar(Integer idLocal, String origen, MultipartFile archivo);
}
