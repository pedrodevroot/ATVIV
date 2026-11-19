package com.autobots.automanager.adaptadores;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.autobots.automanager.enumeracoes.PerfilUsuario;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long usuarioId;
    private final Long credencialId;
    private final String nomeUsuario;
    private final String senha;
    private final Set<PerfilUsuario> perfis;
    private final boolean ativo;

    public UserDetailsImpl(Long usuarioId, Long credencialId, String nomeUsuario, String senha,
            Set<PerfilUsuario> perfis, boolean ativo) {
        this.usuarioId = usuarioId;
        this.credencialId = credencialId;
        this.nomeUsuario = nomeUsuario;
        this.senha = senha;
        this.perfis = perfis == null ? Set.of() : Set.copyOf(perfis);
        this.ativo = ativo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getCredencialId() {
        return credencialId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> autoridades = perfis.stream()
                .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.name()))
                .collect(Collectors.toList());
        return List.copyOf(autoridades);
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return nomeUsuario;
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
        return ativo;
    }
}
