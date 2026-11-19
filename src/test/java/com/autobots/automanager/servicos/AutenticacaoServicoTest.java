package com.autobots.automanager.servicos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.dtos.TokenDto;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.jwt.ProvedorJwt;
import com.autobots.automanager.repositorios.CredencialRepositorio;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServicoTest {

    @Mock
    private AuthenticationManager gerenciador;
    @Mock
    private ProvedorJwt provedorJwt;
    @Mock
    private CredencialRepositorio credencialRepositorio;

    @InjectMocks
    private AutenticacaoServico servico;

    @Test
    @DisplayName("sucesso devolve tipo Bearer, token, expiracao e usuarioId, e grava ultimoAcesso")
    void autenticarComSucesso() {
        UserDetailsImpl usuario = new UserDetailsImpl(3L, 7L, "ana", "hash", Set.of(PerfilUsuario.CLIENTE), true);
        Authentication autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        CredencialUsuarioSenha credencial = new CredencialUsuarioSenha();
        credencial.setId(7L);
        Date expiracao = new Date();
        when(gerenciador.authenticate(any(Authentication.class))).thenReturn(autenticacao);
        when(credencialRepositorio.findById(7L)).thenReturn(Optional.of(credencial));
        when(provedorJwt.proverJwt("ana")).thenReturn("a.b.c");
        when(provedorJwt.obterExpiracao("a.b.c")).thenReturn(expiracao);

        TokenDto token = servico.autenticar("ana", "123456");

        assertEquals(AutenticacaoServico.TIPO, token.getTipo());
        assertEquals("Bearer", token.getTipo());
        assertEquals("a.b.c", token.getToken());
        assertSame(expiracao, token.getExpiracao());
        assertEquals(3L, token.getUsuarioId());
        assertNotNull(credencial.getUltimoAcesso());
        verify(credencialRepositorio).save(credencial);
    }

    @Test
    @DisplayName("BadCredentialsException propaga e nenhum token e gerado")
    void credenciaisInvalidas() {
        when(gerenciador.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> servico.autenticar("ana", "errada"));
        verify(provedorJwt, never()).proverJwt(anyString());
        verify(credencialRepositorio, never()).save(any());
    }
}
