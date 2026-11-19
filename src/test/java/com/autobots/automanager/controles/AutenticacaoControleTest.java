package com.autobots.automanager.controles;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;

import com.autobots.automanager.configuracao.SegurancaTesteConfiguracao;
import com.autobots.automanager.dtos.TokenDto;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkToken;
import com.autobots.automanager.servicos.AutenticacaoServico;

@WebMvcTest(AutenticacaoControle.class)
@Import({ AdicionadorLinkToken.class, SegurancaTesteConfiguracao.class })
class AutenticacaoControleTest {

    private static final String MENSAGEM_FALHA = "Nome de usuario ou senha invalidos, ou credencial inativa";
    private static final String CORPO = "{ \"nomeUsuario\": \"ana\", \"senha\": \"123456\" }";

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private AutenticacaoServico servico;

    private TokenDto token() {
        TokenDto token = new TokenDto();
        token.setTipo("Bearer");
        token.setToken("a.b.c");
        token.setExpiracao(new Date());
        token.setUsuarioId(4L);
        return token;
    }

    @Test
    @DisplayName("POST /login: 200 com header Authorization, token, tipo e link usuario")
    void loginOk() throws Exception {
        when(servico.autenticar("ana", "123456")).thenReturn(token());

        requisicao.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer a.b.c"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").value("a.b.c"))
                .andExpect(jsonPath("$.usuarioId").value(4))
                .andExpect(jsonPath("$._links.usuario.href").value("http://localhost/usuario/4"));
    }

    @Test
    @DisplayName("POST /login: corpo sem campos responde 400 com a lista erros")
    void loginCorpoVazio() throws Exception {
        requisicao.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("nomeUsuario")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("senha")));
        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /login: senha com 73 caracteres responde 400")
    void loginSenhaLonga() throws Exception {
        String corpo = "{ \"nomeUsuario\": \"ana\", \"senha\": \"" + "a".repeat(73) + "\" }";

        requisicao.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("senha")));
        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /login: BadCredentialsException responde 401 com a mensagem generica")
    void loginSenhaErrada() throws Exception {
        when(servico.autenticar(anyString(), anyString())).thenThrow(new BadCredentialsException("Bad credentials"));

        requisicao.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(MENSAGEM_FALHA))
                .andExpect(jsonPath("$.caminho").value("/login"));
    }

    @Test
    @DisplayName("POST /login: DisabledException responde 401 com a mensagem generica")
    void loginCredencialInativa() throws Exception {
        when(servico.autenticar(anyString(), anyString())).thenThrow(new DisabledException("User is disabled"));

        requisicao.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(MENSAGEM_FALHA));
    }

    @Test
    @DisplayName("GET /usuario/1 sem token: 401 com WWW-Authenticate Bearer e mensagem NAO_AUTENTICADO")
    void rotaProtegidaSemToken() throws Exception {
        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO))
                .andExpect(jsonPath("$.caminho").value("/usuario/1"));
    }
}
