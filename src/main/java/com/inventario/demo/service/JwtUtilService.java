package com.inventario.demo.service;

import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtUtilService {

	private static final long JWT_TIME_VALIDITY = 1000L * 60 * 60;
	private static final long JWT_TIME_REFRESH_VALIDATE = 1000L * 60 * 60 * 24 * 7;

	@Value("${app.jwt.secret}")
	private String jwtSecretBase64;

	private SecretKey signingKey;

	@PostConstruct
	void validarClave() {
		try {
			byte[] decoded = Base64.getDecoder().decode(jwtSecretBase64 == null ? "" : jwtSecretBase64.trim());
			if (decoded.length < 32) {
				throw new IllegalStateException(
						"app.jwt.secret debe tener al menos 256 bits (32 bytes en base64). Revisa la variable JWT_SECRET.");
			}
			signingKey = Keys.hmacShaKeyFor(decoded);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("app.jwt.secret no es base64 valido. Revisa la variable JWT_SECRET.", e);
		}
	}

	public String generateToken(UserDetails userDetails, String tipo) {
		return build(userDetails, tipo, JWT_TIME_VALIDITY);
	}

	public String generateRefreshToken(UserDetails userDetails, String tipo) {
		return build(userDetails, tipo, JWT_TIME_REFRESH_VALIDATE);
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	private String build(UserDetails userDetails, String tipo, long validity) {
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.claim("tipo", tipo)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + validity))
				.signWith(getSigningKey())
				.compact();
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	private boolean isTokenExpired(String token) {
		Date expiration = extractClaim(token, Claims::getExpiration);
		return expiration.before(new Date());
	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}

	private SecretKey getSigningKey() {
		return signingKey;
	}
}
