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
import com.autobots.automanager.conversores.TelefoneConversor;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkTelefone;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.servicos.TelefoneServico;

@WebMvcTest(TelefoneControle.class)
@Import({ TelefoneConversor.class, AdicionadorLinkTelefone.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class TelefoneControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private TelefoneServico servico;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private EmpresaRepositorio empresaRepositorio;

    private Telefone telefone(Long id, String ddd, String numero) {
        Telefone telefone = new Telefone();
        telefone.setId(id);
        telefone.setDdd(ddd);
        telefone.setNumero(numero);
        return telefone;
    }

    @Test
    @DisplayName("GET /usuario/{id}/telefones: 200 com a lista")
    void listarOk() throws Exception {
        when(servico.listarPorUsuario(1L))
                .thenReturn(List.of(telefone(1L, "21", "981234576")));

        requisicao.perform(get("/usuario/1/telefones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ddd").value("21"));
    }

    @Test
    @DisplayName("GET /usuario/{id}/telefones: 404 quando o usuario nao existe")
    void listarUsuarioInexistente() throws Exception {
        when(servico.listarPorUsuario(999L))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        requisicao.perform(get("/usuario/999/telefones"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/telefones: 201 com header Location")
    void criarOk() throws Exception {
        when(servico.criar(anyLong(), any(Telefone.class)))
                .thenReturn(telefone(5L, "13", "981234567"));

        String corpo = """
                { "ddd": "13", "numero": "981234567" }
                """;

        requisicao.perform(post("/usuario/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/telefone/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("POST /usuario/{id}/telefones: 404 quando o usuario nao existe")
    void criarUsuarioInexistente() throws Exception {
        when(servico.criar(anyLong(), any(Telefone.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        String corpo = """
                { "ddd": "13", "numero": "981234567" }
                """;

        requisicao.perform(post("/usuario/999/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /usuario/{id}/telefones: 400 quando ddd e numero sao invalidos")
    void criarInvalido() throws Exception {
        String corpo = """
                { "ddd": "1", "numero": "abc" }
                """;

        requisicao.perform(post("/usuario/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("ddd")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("numero")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/{id}/telefones: 400 quando o ddd nao e informado")
    void criarSemDdd() throws Exception {
        String corpo = """
                { "numero": "981234567" }
                """;

        requisicao.perform(post("/usuario/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("ddd")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /telefone/{id}: 200 com o telefone")
    void obterPorIdOk() throws Exception {
        when(servico.obterPorId(2L)).thenReturn(telefone(2L, "21", "981234576"));

        requisicao.perform(get("/telefone/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.numero").value("981234576"));
    }

    @Test
    @DisplayName("GET /telefone/{id}: 404 quando nao existe")
    void obterPorIdInexistente() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Telefone", 999L));

        requisicao.perform(get("/telefone/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Telefone de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("PUT /telefone/{id}: 200 aceitando atualizacao parcial")
    void atualizarParcial() throws Exception {
        when(servico.atualizar(anyLong(), any(Telefone.class)))
                .thenReturn(telefone(2L, "99", "981234576"));

        String corpo = """
                { "ddd": "99" }
                """;

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ddd").value("99"))
                .andExpect(jsonPath("$.numero").value("981234576"));
    }

    @Test
    @DisplayName("PUT /telefone/{id}: 400 quando o numero e invalido")
    void atualizarInvalido() throws Exception {
        String corpo = """
                { "numero": "123" }
                """;

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("numero")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /telefone/{id}: 404 quando nao existe")
    void atualizarInexistente() throws Exception {
        when(servico.atualizar(anyLong(), any(Telefone.class)))
                .thenThrow(new RecursoNaoEncontradoException("Telefone", 999L));

        String corpo = """
                { "ddd": "99" }
                """;

        requisicao.perform(put("/telefone/999")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /telefone/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        requisicao.perform(delete("/telefone/2"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(2L);
    }

    @Test
    @DisplayName("DELETE /telefone/{id}: 404 quando nao existe")
    void excluirInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Telefone", 999L)).when(servico).excluir(999L);

        requisicao.perform(delete("/telefone/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /telefone/{id}: link self do telefone")
    void obterPorIdComLinks() throws Exception {
        when(servico.obterPorId(2L)).thenReturn(telefone(2L, "21", "981234576"));

        requisicao.perform(get("/telefone/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/telefone/2"));
    }

    @Test
    @DisplayName("GET /usuario/{id}/telefones: link self em cada telefone da lista")
    void listarComLinks() throws Exception {
        when(servico.listarPorUsuario(1L))
                .thenReturn(List.of(telefone(1L, "21", "981234576"), telefone(2L, "13", "981234567")));

        requisicao.perform(get("/usuario/1/telefones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].links[0].rel").value("self"))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/telefone/1"))
                .andExpect(jsonPath("$[1].links[0].rel").value("self"))
                .andExpect(jsonPath("$[1].links[0].href").value("http://localhost/telefone/2"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/telefones: resposta com link self do telefone criado")
    void criarComLinks() throws Exception {
        when(servico.criar(anyLong(), any(Telefone.class)))
                .thenReturn(telefone(5L, "13", "981234567"));

        String corpo = """
                { "ddd": "13", "numero": "981234567" }
                """;

        requisicao.perform(post("/usuario/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/telefone/5"));
    }

    @Test
    @DisplayName("PUT /telefone/{id}: resposta com link self do telefone atualizado")
    void atualizarComLinks() throws Exception {
        when(servico.atualizar(anyLong(), any(Telefone.class)))
                .thenReturn(telefone(2L, "99", "981234576"));

        String corpo = """
                { "ddd": "99" }
                """;

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/telefone/2"));
    }

    @Test
    @DisplayName("GET /telefone/{id}: resposta de erro nao tem links")
    void erroSemLinks() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Telefone", 999L));

        requisicao.perform(get("/telefone/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$._links").doesNotExist())
                .andExpect(jsonPath("$.links").doesNotExist());
    }

    @Test
    @DisplayName("GET /empresa/{id}/telefones: 200 com a lista e link self")
    void listarDaEmpresaOk() throws Exception {
        when(servico.listarPorEmpresa(1L)).thenReturn(List.of(telefone(1L, "11", "986454527")));

        requisicao.perform(get("/empresa/1/telefones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/telefone/1"));
    }

    @Test
    @DisplayName("GET /empresa/{id}/telefones: 404 quando a empresa nao existe")
    void listarDaEmpresaInexistente() throws Exception {
        when(servico.listarPorEmpresa(999L))
                .thenThrow(new RecursoNaoEncontradoException("Empresa", 999L));

        requisicao.perform(get("/empresa/999/telefones"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /empresa/{id}/telefones: 201 com header Location")
    void criarNaEmpresaOk() throws Exception {
        when(servico.criarNaEmpresa(anyLong(), any(Telefone.class)))
                .thenReturn(telefone(5L, "11", "986454527"));

        String corpo = """
                { "ddd": "11", "numero": "986454527" }
                """;

        requisicao.perform(post("/empresa/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/telefone/5"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/telefone/5"));
    }

    @Test
    @DisplayName("POST /empresa/{id}/telefones: 400 quando ddd e numero sao invalidos")
    void criarNaEmpresaInvalido() throws Exception {
        String corpo = """
                { "ddd": "1", "numero": "abc" }
                """;

        requisicao.perform(post("/empresa/1/telefones")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("ddd")));

        verifyNoInteractions(servico);
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
    @DisplayName("GET /telefone/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/telefone/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /telefone/{id}: CLIENTE le o do proprio cadastro e recebe 200")
    void autorizacaoClienteLeProprio() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.obterPorId(2L)).thenReturn(telefone(2L, "21", "981234576"));

        requisicao.perform(get("/telefone/2").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /telefone/{id}: CLIENTE le o do cadastro de outro usuario e recebe 403")
    void autorizacaoClienteLeDeOutro() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/telefone/2").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /usuario/{id}/telefones: CLIENTE lista os do proprio cadastro e recebe 200")
    void autorizacaoClienteListaDoProprio() throws Exception {
        when(usuarioRepositorio.findById(3L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.listarPorUsuario(3L)).thenReturn(List.of(telefone(2L, "21", "981234576")));

        requisicao.perform(get("/usuario/3/telefones").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PUT /telefone/{id}: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteAltera() throws Exception {
        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST /usuario/{id}/telefones: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteCadastra() throws Exception {
        requisicao.perform(post("/usuario/3/telefones")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"13\", \"numero\": \"981234567\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /telefone/{id}: VENDEDOR altera o de um gerente e recebe 403")
    void autorizacaoVendedorAlteraDeGerente() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /telefone/{id}: VENDEDOR altera o de um cliente e recebe 200")
    void autorizacaoVendedorAlteraDeCliente() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));
        when(servico.atualizar(anyLong(), any(Telefone.class))).thenReturn(telefone(2L, "21", "981234576"));

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /telefone/{id}: GERENTE altera o de um vendedor e recebe 200")
    void autorizacaoGerenteAlteraDeVendedor() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));
        when(servico.atualizar(anyLong(), any(Telefone.class))).thenReturn(telefone(2L, "21", "981234576"));

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /telefone/{id}: GERENTE exclui o de um administrador e recebe 403")
    void autorizacaoGerenteExcluiDeAdministrador() throws Exception {
        when(usuarioRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(delete("/telefone/2"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /empresa/{id}/telefones: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteListaTelefonesDaEmpresa() throws Exception {
        requisicao.perform(get("/empresa/1/telefones"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /empresa/{id}/telefones: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteCadastraTelefoneDaEmpresa() throws Exception {
        requisicao.perform(post("/empresa/1/telefones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ddd\": \"13\", \"numero\": \"981234567\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("GET /empresa/{id}/telefones: ADMINISTRADOR recebe 200")
    void autorizacaoAdministradorListaTelefonesDaEmpresa() throws Exception {
        when(servico.listarPorEmpresa(1L)).thenReturn(List.of(telefone(1L, "21", "981234576")));

        requisicao.perform(get("/empresa/1/telefones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /telefone/{id}: GERENTE altera telefone que pertence a empresa e recebe 403")
    void autorizacaoGerenteAlteraTelefoneDaEmpresa() throws Exception {
        when(empresaRepositorio.findByTelefonesId(2L)).thenReturn(Optional.of(new Empresa()));

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("PUT /telefone/{id}: ADMINISTRADOR altera telefone de empresa e recebe 200")
    void autorizacaoAdministradorAlteraTelefoneDaEmpresa() throws Exception {
        when(servico.atualizar(anyLong(), any(Telefone.class))).thenReturn(telefone(2L, "99", "981234576"));

        requisicao.perform(put("/telefone/2")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"ddd\": \"99\" }"))
                .andExpect(status().isOk());
    }
}
