package com.inventario.demo.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Freno simple anti fuerza-bruta para el login: maximo 5 intentos fallidos por
 * usuario+IP en 10 minutos. En memoria (un reinicio limpia los contadores,
 * aceptable para una tienda).
 */
@Service
public class LoginAttemptService {

	private static final int MAX_INTENTOS = 5;
	private static final long VENTANA_MS = 10L * 60 * 1000;

	private final Map<String, int[]> intentos = new ConcurrentHashMap<>();
	private final Map<String, Long> ventanaInicio = new ConcurrentHashMap<>();

	public boolean bloqueado(String clave) {
		Long inicio = ventanaInicio.get(clave);
		if (inicio == null) {
			return false;
		}
		if (System.currentTimeMillis() - inicio > VENTANA_MS) {
			intentos.remove(clave);
			ventanaInicio.remove(clave);
			return false;
		}
		int[] contador = intentos.get(clave);
		return contador != null && contador[0] >= MAX_INTENTOS;
	}

	public void registrarFallo(String clave) {
		long ahora = System.currentTimeMillis();
		ventanaInicio.compute(clave, (k, inicio) -> (inicio == null || ahora - inicio > VENTANA_MS) ? ahora : inicio);
		intentos.compute(clave, (k, contador) -> {
			if (contador == null || ahora - ventanaInicio.get(k) > VENTANA_MS) {
				return new int[] { 1 };
			}
			contador[0]++;
			return contador;
		});
	}

	public void registrarExito(String clave) {
		intentos.remove(clave);
		ventanaInicio.remove(clave);
	}
}
