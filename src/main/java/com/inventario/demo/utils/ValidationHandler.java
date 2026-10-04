package com.inventario.demo.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationHandler {

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public List<String> handleValidationErrors(MethodArgumentNotValidException e) {
		List<String> errors = new ArrayList<>();
		List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
		allErrors.forEach(err -> {
			FieldError fe = (FieldError) err;
			errors.add(fe.getDefaultMessage());
		});
		return errors;
	}

	@ExceptionHandler(ModeloNotFoundException.class)
	public ResponseEntity<MensajeResponse> manejarNoEncontrado(ModeloNotFoundException ex) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje(ex.getMessage()).object(null).build(),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<MensajeResponse> manejarRegla(ReglaNegocioException ex) {
		return new ResponseEntity<>(MensajeResponse.builder().mensaje(ex.getMessage()).object(null).build(),
				ex.getStatus());
	}
}
