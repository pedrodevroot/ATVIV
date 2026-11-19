package com.autobots.automanager.filtros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.jwt.ProvedorJwt;

@ExtendWith(MockitoExtension.class)
class AutorizadorTest {

    @Mock
    private ProvedorJwt provedorJwt;
    @Mock
    private UserDetailsService servico;

    private Autorizador autorizador;
    private MockHttpServletRequest requisicao;
    private MockHttpServletResponse resposta;
    private MockFilterChain cadeia;

    @BeforeEach
    void preparar() {
        autorizador = new Autorizador(provedorJwt, servico);
        requisicao = new MockHttpServletRequest("GET", "/usuario/1");
        resposta = new MockHttpServletResponse();
        cadeia = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private UserDetailsImpl usuario(boolean ativo) {
        return new UserDetailsImpl(3L, 7L, "ana", "hash", Set.of(PerfilUsuario.CLIENTE), ativo);
    }

    private void filtrar() throws Exception {
        autorizador.doFilter(requisicao, resposta, cadeia);
    }

    @Test
    @DisplayName("sem header nao autentica e segue a cadeia")
    void semCabecalho() throws Exception {
        filtrar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadeia.getRequest());
        verifyNoInteractions(provedorJwt, servico);
    }

    @Test
    @DisplayName("header Basic nao autentica e segue a cadeia")
    void cabecalhoBasic() throws Exception {
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Basic YW5hOjEyMzQ1Ng==");

        filtrar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadeia.getRequest());
        verifyNoInteractions(provedorJwt, servico);
    }

    @Test
    @DisplayName("token invalido nao autentica e segue a cadeia")
    void tokenInvalido() throws Exception {
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-ruim");
        when(provedorJwt.validarJwt("token-ruim")).thenReturn(false);

        filtrar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadeia.getRequest());
        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("token valido e usuario ativo autenticam com o UserDetailsImpl e as autoridades")
    void tokenValido() throws Exception {
        UserDetailsImpl usuario = usuario(true);
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   token-bom  ");
        when(provedorJwt.validarJwt("token-bom")).thenReturn(true);
        when(provedorJwt.obterNomeUsuario("token-bom")).thenReturn("ana");
        when(servico.loadUserByUsername("ana")).thenReturn(usuario);

        filtrar();

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacao);
        assertSame(usuario, autenticacao.getPrincipal());
        List<String> autoridades = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertEquals(List.of("ROLE_CLIENTE"), autoridades);
        assertNotNull(autenticacao.getDetails());
        assertNotNull(cadeia.getRequest());
    }

    @Test
    @DisplayName("usuario inativo nao autentica e segue a cadeia")
    void usuarioInativo() throws Exception {
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-bom");
        when(provedorJwt.validarJwt("token-bom")).thenReturn(true);
        when(provedorJwt.obterNomeUsuario("token-bom")).thenReturn("ana");
        when(servico.loadUserByUsername("ana")).thenReturn(usuario(false));

        filtrar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadeia.getRequest());
    }

    @Test
    @DisplayName("UsernameNotFoundException nao autentica e segue a cadeia")
    void usuarioInexistente() throws Exception {
        requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-bom");
        when(provedorJwt.validarJwt("token-bom")).thenReturn(true);
        when(provedorJwt.obterNomeUsuario("token-bom")).thenReturn("ana");
        when(servico.loadUserByUsername("ana")).thenThrow(new UsernameNotFoundException("nao encontrado"));

        filtrar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadeia.getRequest());
    }
}
