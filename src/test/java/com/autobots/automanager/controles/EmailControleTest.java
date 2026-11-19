package com.autobots.automanager.controles;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.configuracao.SegurancaTesteConfiguracao;
import com.autobots.automanager.conversores.EmailConversor;
import com.autobots.automanager.entidades.Email;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkEmail;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.servicos.EmailServico;

@WebMvcTest(EmailControle.class)
@Import({ EmailConversor.class, AdicionadorLinkEmail.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class EmailControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private EmailServico servico;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private Email email(Long id, String endereco) {
        Email email = new Email();
        email.setId(id);
        email.setEndereco(endereco);
        return email;
    }

    @Test
    @DisplayName("GET /usuario/{id}/emails: 200 com a lista e link self em cada e-mail")
    void listarOk() throws Exception {
        when(servico.listarPorUsuario(1L)).thenReturn(List.of(email(1L, "maria@email.com")));

        requisicao.perform(get("/usuario/1/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endereco").value("maria@email.com"))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/email/1"));
    }

    @Test
    @DisplayName("GET /usuario/{id}/emails: 404 quando o usuario nao existe")
    void listarUsuarioInexistente() throws Exception {
        when(servico.listarPorUsuario(999L))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        requisicao.perform(get("/usuario/999/emails"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/emails: 201 com header Location e link self")
    void criarOk() throws Exception {
        when(servico.criar(anyLong(), any(Email.class))).thenReturn(email(5L, "maria@email.com"));

        String corpo = """
                { "endereco": "maria@email.com" }
                """;

        requisicao.perform(post("/usuario/1/emails")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/email/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/email/5"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/emails: 400 quando o e-mail nao e informado")
    void criarSemEndereco() throws Exception {
        requisicao.perform(post("/usuario/1/emails")
                .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/{id}/emails: 400 quando o formato do e-mail e invalido")
    void criarFormatoInvalido() throws Exception {
        String corpo = """
                { "endereco": "maria.email.com" }
                """;

        requisicao.perform(post("/usuario/1/emails")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("O endereco de e-mail e invalido"));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /email/{id}: 200 com o e-mail")
    void obterPorIdOk() throws Exception {
        when(servico.obterPorId(2L)).thenReturn(email(2L, "maria@email.com"));

        requisicao.perform(get("/email/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco").value("maria@email.com"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/email/2"));
    }

    @Test
    @DisplayName("GET /email/{id}: 404 quando nao existe, sem links")
    void obterPorIdInexistente() throws Exception {
        when(servico.obterPorId(999L)).thenThrow(new RecursoNaoEncontradoException("Email", 999L));

        requisicao.perform(get("/email/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Email de id 999 nao foi encontrado"))
                .andExpect(jsonPath("$._links").doesNotExist());
    }

    @Test
    @DisplayName("PUT /email/{id}: 200 com o e-mail atualizado")
    void atualizarOk() throws Exception {
        when(servico.atualizar(anyLong(), any(Email.class))).thenReturn(email(2L, "nova@email.com"));

        String corpo = """
                { "endereco": "nova@email.com" }
                """;

        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco").value("nova@email.com"));
    }

    @Test
    @DisplayName("PUT /email/{id}: 400 quando o formato do e-mail e invalido")
    void atualizarFormatoInvalido() throws Exception {
        String corpo = """
                { "endereco": "sem arroba" }
                """;

        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("DELETE /email/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        requisicao.perform(delete("/email/2"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(2L);
    }

    @Test
    @DisplayName("DELETE /email/{id}: 404 quando nao existe")
    void excluirInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Email", 999L)).when(servico).excluir(999L);

        requisicao.perform(delete("/email/999"))
                .andExpect(status().isNotFound());
    }

    private Usuario usuarioComPerfil(Long id, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.getPerfis().add(perfil);
        return usuario;
    }

    private RequestPostProcessor logado(Long id, PerfilUsuario perfil) {
        return user(new UserDetailsImpl(id, id, "usuario" + id, "hash", Set.of(perfil), true));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /email/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/email/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /email/{id}: CLIENTE le o do proprio cadastro e recebe 200")
    void autorizacaoClienteLeProprio() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.obterPorId(2L)).thenReturn(email(2L, "maria@email.com"));

        requisicao.perform(get("/email/2").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /email/{id}: CLIENTE le o do cadastro de outro usuario e recebe 403")
    void autorizacaoClienteLeDeOutro() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/email/2").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /usuario/{id}/emails: CLIENTE lista os do proprio cadastro e recebe 200")
    void autorizacaoClienteListaDoProprio() throws Exception {
        when(usuarioRepositorio.findById(3L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.listarPorUsuario(3L)).thenReturn(List.of(email(2L, "maria@email.com")));

        requisicao.perform(get("/usuario/3/emails").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PUT /email/{id}: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteAltera() throws Exception {
        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"endereco\": \"nova@email.com\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST /usuario/{id}/emails: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteCadastra() throws Exception {
        requisicao.perform(post("/usuario/3/emails")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"endereco\": \"maria@email.com\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /email/{id}: VENDEDOR altera o de um gerente e recebe 403")
    void autorizacaoVendedorAlteraDeGerente() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"endereco\": \"nova@email.com\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /email/{id}: VENDEDOR altera o de um cliente e recebe 200")
    void autorizacaoVendedorAlteraDeCliente() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));
        when(servico.atualizar(anyLong(), any(Email.class))).thenReturn(email(2L, "maria@email.com"));

        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"endereco\": \"nova@email.com\" }"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /email/{id}: GERENTE altera o de um vendedor e recebe 200")
    void autorizacaoGerenteAlteraDeVendedor() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));
        when(servico.atualizar(anyLong(), any(Email.class))).thenReturn(email(2L, "maria@email.com"));

        requisicao.perform(put("/email/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"endereco\": \"nova@email.com\" }"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /email/{id}: GERENTE exclui o de um administrador e recebe 403")
    void autorizacaoGerenteExcluiDeAdministrador() throws Exception {
        when(usuarioRepositorio.findByEmailsId(2L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(delete("/email/2"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }
}
