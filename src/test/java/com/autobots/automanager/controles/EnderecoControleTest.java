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
import com.autobots.automanager.conversores.EnderecoConversor;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.ConflitoException;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkEndereco;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.servicos.EnderecoServico;

@WebMvcTest(EnderecoControle.class)
@Import({ EnderecoConversor.class, AdicionadorLinkEndereco.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class EnderecoControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private EnderecoServico servico;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private EmpresaRepositorio empresaRepositorio;

    private static final String CORPO_ENDERECO = """
            {
              "estado": "Sao Paulo",
              "cidade": "Santos",
              "bairro": "Gonzaga",
              "rua": "Avenida Ana Costa",
              "numero": "250",
              "codigoPostal": "11060002"
            }
            """;

    private Endereco endereco(Long id, String cidade) {
        Endereco endereco = new Endereco();
        endereco.setId(id);
        endereco.setEstado("Sao Paulo");
        endereco.setCidade(cidade);
        endereco.setBairro("Gonzaga");
        endereco.setRua("Avenida Ana Costa");
        endereco.setNumero("250");
        endereco.setCodigoPostal("11060002");
        return endereco;
    }

    @Test
    @DisplayName("GET /usuario/{id}/endereco: 200 com o endereco do usuario e link self")
    void obterDoUsuarioOk() throws Exception {
        when(servico.obterPorUsuario(1L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/usuario/1/endereco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.cidade").value("Santos"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/endereco/3"));
    }

    @Test
    @DisplayName("GET /usuario/{id}/endereco: 404 quando o usuario nao tem endereco")
    void obterDoUsuarioSemEndereco() throws Exception {
        when(servico.obterPorUsuario(1L))
                .thenThrow(new RecursoNaoEncontradoException("O usuario de id 1 nao possui endereco"));

        requisicao.perform(get("/usuario/1/endereco"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("O usuario de id 1 nao possui endereco"));
    }

    @Test
    @DisplayName("GET /usuario/{id}/endereco: 404 quando o usuario nao existe")
    void obterDoUsuarioInexistente() throws Exception {
        when(servico.obterPorUsuario(999L))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        requisicao.perform(get("/usuario/999/endereco"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: 409 quando o usuario ja tem endereco")
    void criarSegundoEndereco() throws Exception {
        when(servico.criar(anyLong(), any(Endereco.class))).thenThrow(new ConflitoException(
                "O usuario de id 1 ja possui endereco. Para altera-lo, use PUT /endereco/3"));

        String corpo = """
                { "cidade": "Santos", "rua": "Rua XV", "numero": "1" }
                """;

        requisicao.perform(post("/usuario/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("O usuario de id 1 ja possui endereco. Para altera-lo, use PUT /endereco/3"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: 201 com header Location")
    void criarOk() throws Exception {
        when(servico.criar(anyLong(), any(Endereco.class))).thenReturn(endereco(5L, "Santos"));

        String corpo = """
                {
                  "estado": "Sao Paulo",
                  "cidade": "Santos",
                  "bairro": "Gonzaga",
                  "rua": "Avenida Ana Costa",
                  "numero": "250",
                  "codigoPostal": "11060002"
                }
                """;

        requisicao.perform(post("/usuario/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/endereco/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: 404 quando o usuario nao existe")
    void criarUsuarioInexistente() throws Exception {
        when(servico.criar(anyLong(), any(Endereco.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        String corpo = """
                { "cidade": "Santos", "rua": "Rua XV", "numero": "1" }
                """;

        requisicao.perform(post("/usuario/999/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: 400 quando cidade, rua ou numero faltam")
    void criarIncompleto() throws Exception {
        String corpo = """
                { "bairro": "Centro" }
                """;

        requisicao.perform(post("/usuario/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("cidade")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("rua")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("numero")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: 400 quando o codigo postal nao tem 8 digitos")
    void criarCodigoPostalInvalido() throws Exception {
        String corpo = """
                { "cidade": "Santos", "rua": "Rua XV", "numero": "1", "codigoPostal": "123" }
                """;

        requisicao.perform(post("/usuario/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("codigoPostal")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /endereco/{id}: 200 com o endereco")
    void obterPorIdOk() throws Exception {
        when(servico.obterPorId(3L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/endereco/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.cidade").value("Santos"))
                .andExpect(jsonPath("$.codigoPostal").value("11060002"));
    }

    @Test
    @DisplayName("GET /endereco/{id}: 404 quando nao existe")
    void obterPorIdInexistente() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Endereco", 999L));

        requisicao.perform(get("/endereco/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Endereco de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("PUT /endereco/{id}: 200 aceitando atualizacao parcial")
    void atualizarParcial() throws Exception {
        when(servico.atualizar(anyLong(), any(Endereco.class)))
                .thenReturn(endereco(3L, "Rio de Janeiro"));

        String corpo = """
                { "cidade": "Rio de Janeiro" }
                """;

        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.rua").value("Avenida Ana Costa"));
    }

    @Test
    @DisplayName("PUT /endereco/{id}: 400 quando o codigo postal e invalido")
    void atualizarCodigoPostalInvalido() throws Exception {
        String corpo = """
                { "codigoPostal": "123" }
                """;

        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("codigoPostal")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /endereco/{id}: 404 quando nao existe")
    void atualizarInexistente() throws Exception {
        when(servico.atualizar(anyLong(), any(Endereco.class)))
                .thenThrow(new RecursoNaoEncontradoException("Endereco", 999L));

        String corpo = """
                { "cidade": "Santos" }
                """;

        requisicao.perform(put("/endereco/999")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /endereco/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        requisicao.perform(delete("/endereco/3"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(3L);
    }

    @Test
    @DisplayName("DELETE /endereco/{id}: 404 quando nao existe")
    void excluirInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Endereco", 999L)).when(servico).excluir(999L);

        requisicao.perform(delete("/endereco/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /endereco/{id}: link self do endereco")
    void obterPorIdComLinks() throws Exception {
        when(servico.obterPorId(3L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/endereco/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/endereco/3"));
    }

    @Test
    @DisplayName("POST /usuario/{id}/endereco: resposta com link self do endereco criado")
    void criarComLinks() throws Exception {
        when(servico.criar(anyLong(), any(Endereco.class))).thenReturn(endereco(5L, "Santos"));

        String corpo = """
                { "cidade": "Santos", "rua": "Avenida Ana Costa", "numero": "250" }
                """;

        requisicao.perform(post("/usuario/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/endereco/5"));
    }

    @Test
    @DisplayName("PUT /endereco/{id}: resposta com link self do endereco atualizado")
    void atualizarComLinks() throws Exception {
        when(servico.atualizar(anyLong(), any(Endereco.class)))
                .thenReturn(endereco(3L, "Rio de Janeiro"));

        String corpo = """
                { "cidade": "Rio de Janeiro" }
                """;

        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/endereco/3"));
    }

    @Test
    @DisplayName("GET /endereco/{id}: resposta de erro nao tem links")
    void erroSemLinks() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Endereco", 999L));

        requisicao.perform(get("/endereco/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$._links").doesNotExist())
                .andExpect(jsonPath("$.links").doesNotExist());
    }

    @Test
    @DisplayName("GET /empresa/{id}/endereco: 200 com o endereco da empresa e link self")
    void obterDaEmpresaOk() throws Exception {
        when(servico.obterPorEmpresa(1L)).thenReturn(endereco(3L, "Sao Paulo"));

        requisicao.perform(get("/empresa/1/endereco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/endereco/3"));
    }

    @Test
    @DisplayName("GET /empresa/{id}/endereco: 404 quando a empresa nao tem endereco")
    void obterDaEmpresaSemEndereco() throws Exception {
        when(servico.obterPorEmpresa(1L))
                .thenThrow(new RecursoNaoEncontradoException("A empresa de id 1 nao possui endereco"));

        requisicao.perform(get("/empresa/1/endereco"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("A empresa de id 1 nao possui endereco"));
    }

    @Test
    @DisplayName("POST /empresa/{id}/endereco: 201 com header Location")
    void criarNaEmpresaOk() throws Exception {
        when(servico.criarNaEmpresa(anyLong(), any(Endereco.class))).thenReturn(endereco(5L, "Sao Paulo"));

        String corpo = """
                { "cidade": "Sao Paulo", "rua": "Avenida Sao Joao", "numero": "100" }
                """;

        requisicao.perform(post("/empresa/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/endereco/5"));
    }

    @Test
    @DisplayName("POST /empresa/{id}/endereco: 409 quando a empresa ja tem endereco")
    void criarSegundoEnderecoNaEmpresa() throws Exception {
        when(servico.criarNaEmpresa(anyLong(), any(Endereco.class))).thenThrow(new ConflitoException(
                "A empresa de id 1 ja possui endereco. Para altera-lo, use PUT /endereco/3"));

        String corpo = """
                { "cidade": "Sao Paulo", "rua": "Avenida Sao Joao", "numero": "100" }
                """;

        requisicao.perform(post("/empresa/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /empresa/{id}/endereco: 400 quando cidade, rua ou numero faltam")
    void criarNaEmpresaIncompleto() throws Exception {
        String corpo = """
                { "bairro": "Centro" }
                """;

        requisicao.perform(post("/empresa/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("cidade")));

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
    @DisplayName("GET /endereco/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/endereco/3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /empresa/{id}/endereco: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteObtemEnderecoDaEmpresa() throws Exception {
        requisicao.perform(get("/empresa/1/endereco"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /empresa/{id}/endereco: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteCadastraEnderecoDaEmpresa() throws Exception {
        requisicao.perform(post("/empresa/1/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_ENDERECO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("GET /empresa/{id}/endereco: ADMINISTRADOR recebe 200")
    void autorizacaoAdministradorObtemEnderecoDaEmpresa() throws Exception {
        when(servico.obterPorEmpresa(1L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/empresa/1/endereco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("GET /usuario/{id}/endereco: CLIENTE le o proprio endereco e recebe 200")
    void autorizacaoClienteLeProprioEndereco() throws Exception {
        when(usuarioRepositorio.findById(3L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.obterPorUsuario(3L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/usuario/3/endereco").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("Santos"));
    }

    @Test
    @DisplayName("GET /endereco/{id}: CLIENTE le o proprio endereco e recebe 200")
    void autorizacaoClienteLeEnderecoPeloId() throws Exception {
        when(usuarioRepositorio.findByEnderecoId(3L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.obterPorId(3L)).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(get("/endereco/3").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /usuario/{id}/endereco: CLIENTE le o endereco de outro usuario e recebe 403")
    void autorizacaoClienteLeEnderecoDeOutro() throws Exception {
        when(usuarioRepositorio.findById(4L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/usuario/4/endereco").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST /usuario/{id}/endereco: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteCadastraEndereco() throws Exception {
        requisicao.perform(post("/usuario/3/endereco")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_ENDERECO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PUT /endereco/{id}: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteAlteraEndereco() throws Exception {
        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"cidade\": \"Santos\" }"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /endereco/{id}: VENDEDOR altera o de um gerente e recebe 403")
    void autorizacaoVendedorAlteraEnderecoDeGerente() throws Exception {
        when(usuarioRepositorio.findByEnderecoId(3L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"cidade\": \"Santos\" }"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /endereco/{id}: GERENTE altera o de um vendedor e recebe 200")
    void autorizacaoGerenteAlteraEnderecoDeVendedor() throws Exception {
        when(usuarioRepositorio.findByEnderecoId(3L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));
        when(servico.atualizar(anyLong(), any(Endereco.class))).thenReturn(endereco(3L, "Santos"));

        requisicao.perform(put("/endereco/3")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"cidade\": \"Santos\" }"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /endereco/{id}: GERENTE exclui endereco de empresa e recebe 403")
    void autorizacaoGerenteExcluiEnderecoDaEmpresa() throws Exception {
        when(empresaRepositorio.findByEnderecoId(3L)).thenReturn(Optional.of(new Empresa()));

        requisicao.perform(delete("/endereco/3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /endereco/{id}: GERENTE exclui o de um administrador e recebe 403")
    void autorizacaoGerenteExcluiEnderecoDeAdministrador() throws Exception {
        when(usuarioRepositorio.findByEnderecoId(3L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(delete("/endereco/3"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }
}
