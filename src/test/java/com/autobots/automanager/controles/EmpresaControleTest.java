package com.autobots.automanager.controles;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

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

import com.autobots.automanager.configuracao.SegurancaTesteConfiguracao;
import com.autobots.automanager.conversores.EmpresaConversor;
import com.autobots.automanager.conversores.UsuarioConversor;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.ConflitoException;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkEmpresa;
import com.autobots.automanager.modelo.AdicionadorLinkUsuario;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.servicos.EmpresaServico;

@WebMvcTest(EmpresaControle.class)
@Import({ EmpresaConversor.class, UsuarioConversor.class, AdicionadorLinkEmpresa.class,
        AdicionadorLinkUsuario.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class EmpresaControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private EmpresaServico servico;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private Empresa empresa(Long id, String razaoSocial) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setRazaoSocial(razaoSocial);
        empresa.setNomeFantasia("Oficina");
        empresa.setCadastro(Calendar.getInstance().getTime());
        Telefone telefone = new Telefone();
        telefone.setId(1L);
        telefone.setDdd("11");
        telefone.setNumero("986454527");
        empresa.getTelefones().add(telefone);
        return empresa;
    }

    private Usuario usuario(Long id, Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Maria");
        usuario.getPerfis().add(PerfilUsuario.CLIENTE);
        usuario.setEmpresa(empresa);
        return usuario;
    }

    @Test
    @DisplayName("GET /empresa/{id}: 200 com a empresa, sem expor as colecoes do negocio")
    void obterPorIdOk() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(empresa(1L, "Car Service Ltda"));

        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.razaoSocial").value("Car Service Ltda"))
                .andExpect(jsonPath("$.cadastro").exists())
                .andExpect(jsonPath("$.telefones[0].ddd").value("11"))
                .andExpect(jsonPath("$.usuarios").doesNotExist())
                .andExpect(jsonPath("$.vendas").doesNotExist());
    }

    @Test
    @DisplayName("GET /empresa/{id}: 404 com corpo de erro e sem links")
    void obterPorIdInexistente() throws Exception {
        when(servico.obterPorId(999L)).thenThrow(new RecursoNaoEncontradoException("Empresa", 999L));

        requisicao.perform(get("/empresa/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa de id 999 nao foi encontrado"))
                .andExpect(jsonPath("$._links").doesNotExist());
    }

    @Test
    @DisplayName("GET /empresa/{id}: links self, empresas, usuarios, telefones, mercadorias, servicos e vendas")
    void obterPorIdComLinks() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(empresa(1L, "Car Service Ltda"));

        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/empresa/1"))
                .andExpect(jsonPath("$._links.empresas.href").value("http://localhost/empresa/empresas"))
                .andExpect(jsonPath("$._links.usuarios.href").value("http://localhost/empresa/1/usuarios"))
                .andExpect(jsonPath("$._links.telefones.href").value("http://localhost/empresa/1/telefones"))
                .andExpect(jsonPath("$._links.mercadorias.href").value("http://localhost/empresa/1/mercadorias"))
                .andExpect(jsonPath("$._links.servicos.href").value("http://localhost/empresa/1/servicos"))
                .andExpect(jsonPath("$._links.vendas.href").value("http://localhost/empresa/1/vendas"))
                .andExpect(jsonPath("$._links.endereco").doesNotExist());
    }

    @Test
    @DisplayName("GET /empresa/{id}: com link de endereco quando a empresa tem endereco")
    void obterPorIdComLinkDeEndereco() throws Exception {
        Empresa empresa = empresa(1L, "Car Service Ltda");
        Endereco endereco = new Endereco();
        endereco.setId(3L);
        endereco.setCidade("Sao Paulo");
        empresa.setEndereco(endereco);
        when(servico.obterPorId(1L)).thenReturn(empresa);

        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$._links.endereco.href").value("http://localhost/empresa/1/endereco"));
    }

    @Test
    @DisplayName("GET /empresa/empresas: 200 com a lista e link self em cada empresa")
    void listarOk() throws Exception {
        when(servico.listar()).thenReturn(List.of(empresa(1L, "Primeira"), empresa(2L, "Segunda")));

        requisicao.perform(get("/empresa/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].razaoSocial").value("Primeira"))
                .andExpect(jsonPath("$[0].links.length()").value(1))
                .andExpect(jsonPath("$[1].links[0].href").value("http://localhost/empresa/2"));
    }

    @Test
    @DisplayName("POST /empresa/cadastro: 201 com header Location e link self")
    void cadastrarOk() throws Exception {
        when(servico.cadastrar(any(Empresa.class))).thenReturn(empresa(2L, "Nova Ltda"));

        String corpo = """
                { "razaoSocial": "Nova Ltda" }
                """;

        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/empresa/2"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/empresa/2"));
    }

    @Test
    @DisplayName("POST /empresa/cadastro: 400 quando a razao social nao e informada")
    void cadastrarSemRazaoSocial() throws Exception {
        String corpo = """
                { "nomeFantasia": "So fantasia" }
                """;

        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("razaoSocial")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /empresa/cadastro: 400 com caminho indexado nos filhos invalidos")
    void cadastrarFilhosInvalidos() throws Exception {
        String corpo = """
                {
                  "razaoSocial": "Invalida",
                  "telefones": [ { "ddd": "1", "numero": "abc" } ],
                  "endereco": { "bairro": "Centro" }
                }
                """;

        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("telefones[0].ddd")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco.cidade")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /empresa/atualizar: 200 com a empresa atualizada")
    void atualizarOk() throws Exception {
        when(servico.atualizar(any(Empresa.class))).thenReturn(empresa(1L, "Nome Novo"));

        String corpo = """
                { "id": 1, "razaoSocial": "Nome Novo" }
                """;

        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Nome Novo"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/empresa/1"));
    }

    @Test
    @DisplayName("PUT /empresa/atualizar: 400 quando o id nao e informado")
    void atualizarSemId() throws Exception {
        String corpo = """
                { "razaoSocial": "Sem Id" }
                """;

        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("id")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /empresa/atualizar: 404 quando a empresa nao existe")
    void atualizarInexistente() throws Exception {
        when(servico.atualizar(any(Empresa.class)))
                .thenThrow(new RecursoNaoEncontradoException("Empresa", 999L));

        String corpo = """
                { "id": 999, "razaoSocial": "Fantasma" }
                """;

        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /empresa/excluir/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        requisicao.perform(delete("/empresa/excluir/1"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(1L);
    }

    @Test
    @DisplayName("DELETE /empresa/excluir/{id}: 404 quando a empresa nao existe")
    void excluirInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Empresa", 999L)).when(servico).excluir(999L);

        requisicao.perform(delete("/empresa/excluir/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /empresa/{id}/usuarios: 200 com os usuarios, o empresaId e o link self")
    void listarUsuariosOk() throws Exception {
        when(servico.listarUsuarios(1L)).thenReturn(List.of(usuario(5L, empresa(1L, "Car Service Ltda"))));

        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].empresaId").value(1))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/usuario/5"));
    }

    @Test
    @DisplayName("GET /empresa/{id}/usuarios: 404 quando a empresa nao existe")
    void listarUsuariosEmpresaInexistente() throws Exception {
        when(servico.listarUsuarios(999L)).thenThrow(new RecursoNaoEncontradoException("Empresa", 999L));

        requisicao.perform(get("/empresa/999/usuarios"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /empresa/{id}/usuarios/{usuarioId}: 204 ao vincular")
    void vincularOk() throws Exception {
        requisicao.perform(put("/empresa/1/usuarios/5"))
                .andExpect(status().isNoContent());

        verify(servico).vincularUsuario(1L, 5L);
    }

    @Test
    @DisplayName("PUT /empresa/{id}/usuarios/{usuarioId}: 409 quando o usuario e de outra empresa")
    void vincularUsuarioDeOutraEmpresa() throws Exception {
        String mensagem = "O usuario de id 5 ja esta vinculado a empresa de id 2. "
                + "Desvincule-o antes de vincular a outra empresa";
        doThrow(new ConflitoException(mensagem)).when(servico).vincularUsuario(1L, 5L);

        requisicao.perform(put("/empresa/1/usuarios/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @Test
    @DisplayName("PUT /empresa/{id}/usuarios/{usuarioId}: 404 quando o usuario nao existe")
    void vincularUsuarioInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Usuario", 999L)).when(servico).vincularUsuario(1L, 999L);

        requisicao.perform(put("/empresa/1/usuarios/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /empresa/{id}/usuarios/{usuarioId}: 204 ao desvincular")
    void desvincularOk() throws Exception {
        requisicao.perform(delete("/empresa/1/usuarios/5"))
                .andExpect(status().isNoContent());

        verify(servico).desvincularUsuario(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /empresa/{id}/usuarios/{usuarioId}: 404 quando o usuario nao e da empresa")
    void desvincularUsuarioDeOutraEmpresa() throws Exception {
        String mensagem = "O usuario de id 5 nao esta vinculado a empresa de id 1";
        doThrow(new RecursoNaoEncontradoException(mensagem)).when(servico).desvincularUsuario(1L, 5L);

        requisicao.perform(delete("/empresa/1/usuarios/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @Test
    @DisplayName("POST /empresa/cadastro: lista de telefones enviada como null vale como vazia")
    void cadastroComTelefonesNulos() throws Exception {
        when(servico.cadastrar(any(Empresa.class))).thenReturn(empresa(2L, "Oficina"));

        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"razaoSocial\": \"Oficina\", \"telefones\": null }"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /empresa/cadastro: 400 com telefone nulo na lista")
    void cadastroComTelefoneNulo() throws Exception {
        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"razaoSocial\": \"Oficina\", \"telefones\": [ null ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("telefones[0]"));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /empresa/atualizar: lista de telefones enviada como null vale como vazia")
    void atualizacaoComTelefonesNulos() throws Exception {
        when(servico.atualizar(any(Empresa.class))).thenReturn(empresa(1L, "Oficina"));

        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"telefones\": null }"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /empresa/atualizar: 400 com telefone nulo na lista")
    void atualizacaoComTelefoneNulo() throws Exception {
        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"telefones\": [ null ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("telefones[0]"));

        verifyNoInteractions(servico);
    }

    private static final String CORPO_EMPRESA = """
            { "razaoSocial": "Nova Ltda" }
            """;

    private Usuario usuarioComPerfil(Long id, PerfilUsuario perfil) {
        Usuario usuario = usuario(id, empresa(1L, "Car Service Ltda"));
        usuario.getPerfis().clear();
        usuario.getPerfis().add(perfil);
        return usuario;
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /empresa/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /empresa/{empresaId}/usuarios: 401 sem autenticacao")
    void autorizacaoListarUsuariosSemAutenticacao() throws Exception {
        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /empresa/empresas: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteListaEmpresas() throws Exception {
        requisicao.perform(get("/empresa/empresas"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /empresa/{id}: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteObtemEmpresa() throws Exception {
        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /empresa/cadastro: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteCadastraEmpresa() throws Exception {
        requisicao.perform(post("/empresa/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_EMPRESA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /empresa/atualizar: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteAtualizaEmpresa() throws Exception {
        requisicao.perform(put("/empresa/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"razaoSocial\": \"Nome Novo\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /empresa/excluir/{id}: GERENTE recebe 403 sem chamar o servico")
    void autorizacaoGerenteExcluiEmpresa() throws Exception {
        requisicao.perform(delete("/empresa/excluir/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("GET /empresa/{id}: VENDEDOR recebe 403 sem chamar o servico")
    void autorizacaoVendedorObtemEmpresa() throws Exception {
        requisicao.perform(get("/empresa/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: GERENTE vincula VENDEDOR e recebe 204")
    void autorizacaoGerenteVinculaVendedor() throws Exception {
        when(usuarioRepositorio.findById(5L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));

        requisicao.perform(put("/empresa/1/usuarios/5"))
                .andExpect(status().isNoContent());

        verify(servico).vincularUsuario(1L, 5L);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: GERENTE vincula ADMINISTRADOR e recebe 403")
    void autorizacaoGerenteVinculaAdministrador() throws Exception {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(put("/empresa/1/usuarios/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: VENDEDOR vincula GERENTE e recebe 403")
    void autorizacaoVendedorVinculaGerente() throws Exception {
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(put("/empresa/1/usuarios/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: VENDEDOR vincula CLIENTE e recebe 204")
    void autorizacaoVendedorVinculaCliente() throws Exception {
        when(usuarioRepositorio.findById(5L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.CLIENTE)));

        requisicao.perform(put("/empresa/1/usuarios/5"))
                .andExpect(status().isNoContent());

        verify(servico).vincularUsuario(1L, 5L);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("DELETE /empresa/{empresaId}/usuarios/{usuarioId}: VENDEDOR desvincula GERENTE e recebe 403")
    void autorizacaoVendedorDesvinculaGerente() throws Exception {
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(delete("/empresa/1/usuarios/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: CLIENTE recebe 403 sem consultar o repositorio")
    void autorizacaoClienteVinculaUsuario() throws Exception {
        requisicao.perform(put("/empresa/1/usuarios/5"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @DisplayName("PUT /empresa/{empresaId}/usuarios/{usuarioId}: ADMINISTRADOR vincula sem consultar o repositorio")
    void autorizacaoAdministradorVinculaUsuario() throws Exception {
        requisicao.perform(put("/empresa/1/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(servico).vincularUsuario(1L, 1L);
        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("GET /empresa/{empresaId}/usuarios: VENDEDOR recebe so os clientes")
    void autorizacaoVendedorListaSoClientes() throws Exception {
        when(servico.listarUsuarios(1L)).thenReturn(List.of(
                usuarioComPerfil(5L, PerfilUsuario.CLIENTE),
                usuarioComPerfil(6L, PerfilUsuario.GERENTE),
                usuarioComPerfil(7L, PerfilUsuario.VENDEDOR),
                usuarioComPerfil(8L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[1].id").value(8));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /empresa/{empresaId}/usuarios: GERENTE nao recebe administradores")
    void autorizacaoGerenteListaSemAdministradores() throws Exception {
        when(servico.listarUsuarios(1L)).thenReturn(List.of(
                usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR),
                usuarioComPerfil(2L, PerfilUsuario.GERENTE),
                usuarioComPerfil(5L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(5));
    }

    @Test
    @DisplayName("GET /empresa/{empresaId}/usuarios: ADMINISTRADOR recebe todos")
    void autorizacaoAdministradorListaTodos() throws Exception {
        when(servico.listarUsuarios(1L)).thenReturn(List.of(
                usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR),
                usuarioComPerfil(5L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET /empresa/{empresaId}/usuarios: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteListaUsuarios() throws Exception {
        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "FORNECEDOR")
    @DisplayName("GET /empresa/{empresaId}/usuarios: FORNECEDOR recebe 403 sem chamar o servico")
    void autorizacaoFornecedorListaUsuarios() throws Exception {
        requisicao.perform(get("/empresa/1/usuarios"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }
}
