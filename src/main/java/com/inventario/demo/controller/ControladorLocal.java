package com.inventario.demo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventario.demo.Dto.LocalDTO;
import com.inventario.demo.interfaces.LocalRepository;
import com.inventario.demo.modelo.Local;
import com.inventario.demo.utils.MensajeResponse;

@RestController
@RequestMapping(value = "/ControladorLocal", produces = MediaType.APPLICATION_JSON_VALUE)
public class ControladorLocal {

	private final LocalRepository localRepository;

	public ControladorLocal(LocalRepository localRepository) {
		this.localRepository = localRepository;
	}

	@GetMapping("/listarLocales")
	public ResponseEntity<MensajeResponse> listar() {
		List<LocalDTO> locales = localRepository.findAll().stream().map(this::aDto).toList();
		return ResponseEntity.ok(MensajeResponse.builder().mensaje("Locales encontrados").object(locales).build());
	}

	private LocalDTO aDto(Local local) {
		LocalDTO dto = new LocalDTO();
		dto.setIdLocal(local.getIdLocal());
		dto.setCodigo(local.getCodigo());
		dto.setNombre(local.getNombre());
		dto.setExigeTalla(local.isExigeTalla());
		return dto;
	}
}
