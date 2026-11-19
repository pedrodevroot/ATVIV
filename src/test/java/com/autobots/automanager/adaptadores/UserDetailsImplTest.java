package com.autobots.automanager.adaptadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.autobots.automanager.enumeracoes.PerfilUsuario;

class UserDetailsImplTest {

    @Test
    @DisplayName("getAuthorities devolve ROLE_ mais o nome de cada perfil")
    void autoridades() {
        UserDetailsImpl usuario = new UserDetailsImpl(1L, 2L, "ana", "hash",
                Set.of(PerfilUsuario.CLIENTE), true);

        List<String> autoridades = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertEquals(List.of("ROLE_CLIENTE"), autoridades);
    }

    @Test
    @DisplayName("isEnabled segue o campo ativo e as demais flags sao verdadeiras")
    void habilitado() {
        UserDetailsImpl ativo = new UserDetailsImpl(1L, 2L, "ana", "hash", Set.of(PerfilUsuario.CLIENTE), true);
        UserDetailsImpl inativo = new UserDetailsImpl(1L, 2L, "ana", "hash", Set.of(PerfilUsuario.CLIENTE), false);

        assertTrue(ativo.isEnabled());
        assertFalse(inativo.isEnabled());
        assertTrue(inativo.isAccountNonExpired());
        assertTrue(inativo.isAccountNonLocked());
        assertTrue(inativo.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("getters devolvem os valores do construtor e as authorities refletem os perfis")
    void getters() {
        Set<PerfilUsuario> perfis = new HashSet<>(Set.of(PerfilUsuario.CLIENTE, PerfilUsuario.FORNECEDOR));
        UserDetailsImpl usuario = new UserDetailsImpl(1L, 2L, "ana", "hash", perfis, true);
        perfis.clear();

        assertEquals(1L, usuario.getUsuarioId());
        assertEquals(2L, usuario.getCredencialId());
        assertEquals("ana", usuario.getUsername());
        assertEquals("hash", usuario.getPassword());
        assertEquals(Set.of("ROLE_CLIENTE", "ROLE_FORNECEDOR"),
                usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
    }
}
