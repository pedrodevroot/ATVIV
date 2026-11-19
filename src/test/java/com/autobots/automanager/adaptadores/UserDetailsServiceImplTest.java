package com.autobots.automanager.adaptadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private CredencialRepositorio credencialRepositorio;
    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @InjectMocks
    private UserDetailsServiceImpl servico;

    private CredencialUsuarioSenha credencial(boolean inativo) {
        CredencialUsuarioSenha credencial = new CredencialUsuarioSenha();
        credencial.setId(7L);
        credencial.setNomeUsuario("ana");
        credencial.setSenha("$2a$10$hash");
        credencial.setInativo(inativo);
        return credencial;
    }

    private Usuario usuario(PerfilUsuario... perfis) {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setPerfis(Set.of(perfis));
        return usuario;
    }

    @Test
    @DisplayName("encontra o usuario e monta o UserDetailsImpl com ids, perfis e senha")
    void carregaUsuario() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(false)));
        when(usuarioRepositorio.findByCredenciaisId(7L)).thenReturn(Optional.of(usuario(PerfilUsuario.CLIENTE)));

        UserDetailsImpl resultado = (UserDetailsImpl) servico.loadUserByUsername("ana");

        assertEquals(3L, resultado.getUsuarioId());
        assertEquals(7L, resultado.getCredencialId());
        assertEquals("ana", resultado.getUsername());
        assertEquals("$2a$10$hash", resultado.getPassword());
        assertEquals(Set.of("ROLE_CLIENTE"), autoridades(resultado));
        assertTrue(resultado.isEnabled());
    }

    @Test
    @DisplayName("credencial inexistente lanca UsernameNotFoundException")
    void credencialInexistente() {
        when(credencialRepositorio.buscarPorNomeUsuario("ninguem")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servico.loadUserByUsername("ninguem"));
        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @DisplayName("credencial sem dono lanca UsernameNotFoundException")
    void credencialSemDono() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(false)));
        when(usuarioRepositorio.findByCredenciaisId(7L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servico.loadUserByUsername("ana"));
    }

    @Test
    @DisplayName("usuario so com FORNECEDOR lanca UsernameNotFoundException")
    void somenteFornecedor() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(false)));
        when(usuarioRepositorio.findByCredenciaisId(7L)).thenReturn(Optional.of(usuario(PerfilUsuario.FORNECEDOR)));

        assertThrows(UsernameNotFoundException.class, () -> servico.loadUserByUsername("ana"));
    }

    @Test
    @DisplayName("usuario sem perfil nenhum lanca UsernameNotFoundException")
    void semPerfil() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(false)));
        when(usuarioRepositorio.findByCredenciaisId(7L)).thenReturn(Optional.of(usuario()));

        assertThrows(UsernameNotFoundException.class, () -> servico.loadUserByUsername("ana"));
    }

    @Test
    @DisplayName("usuario com FORNECEDOR e CLIENTE carrega normalmente")
    void fornecedorECliente() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(false)));
        when(usuarioRepositorio.findByCredenciaisId(7L))
                .thenReturn(Optional.of(usuario(PerfilUsuario.FORNECEDOR, PerfilUsuario.CLIENTE)));

        UserDetailsImpl resultado = (UserDetailsImpl) servico.loadUserByUsername("ana");

        assertEquals(Set.of("ROLE_FORNECEDOR", "ROLE_CLIENTE"), autoridades(resultado));
    }

    @Test
    @DisplayName("credencial inativa gera UserDetails desabilitado")
    void credencialInativa() {
        when(credencialRepositorio.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(credencial(true)));
        when(usuarioRepositorio.findByCredenciaisId(7L)).thenReturn(Optional.of(usuario(PerfilUsuario.CLIENTE)));

        assertFalse(servico.loadUserByUsername("ana").isEnabled());
    }

    private Set<String> autoridades(UserDetails usuario) {
        return usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
