package com.inventario.demo;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.inventario.demo.modelo.Usuario;

public class UsuarioDetails implements UserDetails {

	private final Usuario usuario;

	public UsuarioDetails(Usuario usuario) {
		this.usuario = usuario;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String rol = usuario.getRol() == null ? "USER" : usuario.getRol().getNombreRol();
		return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	@Override
	public String getPassword() {
		return usuario.getClave();
	}

	@Override
	public String getUsername() {
		return usuario.getUsuario();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return usuario.isEstado();
	}
}
