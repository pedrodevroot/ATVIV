package com.autobots.automanager.controles;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.configuracao.SegurancaTesteConfiguracao;
import com.autobots.automanager.conversores.UsuarioConversor;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.ConflitoException;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkUsuario;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.servicos.UsuarioServico;

@WebMvcTest(UsuarioControle.class)
@Import({ UsuarioConversor.class, AdicionadorLinkUsuario.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class UsuarioControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private UsuarioServico servico;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private Usuario usuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setNomeSocial("Dom Pedro");
        usuario.getPerfis().add(PerfilUsuario.CLIENTE);
        Calendar calendario = Calendar.getInstance();
        calendario.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        usuario.setDataCadastro(calendario.getTime());
        Telefone telefone = new Telefone();
        telefone.setId(1L);
        telefone.setDdd("21");
        telefone.setNumero("981234576");
        usuario.getTelefones().add(telefone);
        return usuario;
    }

    @Test
    @DisplayName("GET /usuario/{id}: 200 com o usuario serializado")
    void obterPorIdOk() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(usuario(1L, "Pedro de Alcantara"));

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Pedro de Alcantara"))
                .andExpect(jsonPath("$.perfis[0]").value("CLIENTE"))
                .andExpect(jsonPath("$.dataCadastro").exists())
                .andExpect(jsonPath("$.telefones[0].ddd").value("21"));
    }

    @Test
    @DisplayName("GET /usuario/{id}: perfis em ordem fixa, a do enum")
    void obterPorIdPerfisOrdenados() throws Exception {
        Usuario usuario = usuario(1L, "Varios Perfis");
        usuario.getPerfis().add(PerfilUsuario.FORNECEDOR);
        usuario.getPerfis().add(PerfilUsuario.VENDEDOR);
        when(servico.obterPorId(1L)).thenReturn(usuario);

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfis[0]").value("VENDEDOR"))
                .andExpect(jsonPath("$.perfis[1]").value("CLIENTE"))
                .andExpect(jsonPath("$.perfis[2]").value("FORNECEDOR"));
    }

    @Test
    @DisplayName("GET /usuario/{id}: a senha da credencial nunca aparece na resposta")
    void obterPorIdSemSenha() throws Exception {
        Usuario usuario = usuario(1L, "Com Credencial");
        CredencialUsuarioSenha credencial = new CredencialUsuarioSenha();
        credencial.setId(4L);
        credencial.setNomeUsuario("pedro");
        credencial.setSenha("segredo1");
        usuario.getCredenciais().add(credencial);
        when(servico.obterPorId(1L)).thenReturn(usuario);

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credenciais[0].tipo").value("USUARIO_SENHA"))
                .andExpect(jsonPath("$.credenciais[0].nomeUsuario").value("pedro"))
                .andExpect(jsonPath("$.credenciais[0].senha").doesNotExist());
    }

    @Test
    @DisplayName("GET /usuario/{id}: 404 com corpo de erro quando nao existe")
    void obterPorIdInexistente() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        requisicao.perform(get("/usuario/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"))
                .andExpect(jsonPath("$.caminho").value("/usuario/999"));
    }

    @Test
    @DisplayName("GET /cliente/{id}: 404, as rotas do cliente deram lugar as do usuario")
    void rotaAntigaDoClienteNaoExiste() throws Exception {
        requisicao.perform(get("/cliente/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /usuario/usuarios: 200 com a lista")
    void listarOk() throws Exception {
        when(servico.listar()).thenReturn(List.of(usuario(1L, "Primeiro"), usuario(2L, "Segundo")));

        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Primeiro"))
                .andExpect(jsonPath("$[1].nome").value("Segundo"));
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 201 com header Location")
    void cadastrarOk() throws Exception {
        when(servico.cadastrar(any(Usuario.class))).thenReturn(usuario(2L, "Maria Silva"));

        String corpo = """
                { "nome": "Maria Silva", "perfis": [ "CLIENTE" ] }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/usuario/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 quando o nome nao e informado")
    void cadastrarSemNome() throws Exception {
        String corpo = """
                { "nomeSocial": "So apelido", "perfis": [ "CLIENTE" ] }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("nome")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 quando nenhum perfil e informado")
    void cadastrarSemPerfil() throws Exception {
        String corpo = """
                { "nome": "Sem Perfil" }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("perfis")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 com os valores aceitos quando o perfil nao existe")
    void cadastrarPerfilInexistente() throws Exception {
        String corpo = """
                { "nome": "Perfil Errado", "perfis": [ "FUNCIONARIO" ] }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "O valor FUNCIONARIO nao e aceito. Valores aceitos: [ADMINISTRADOR, GERENTE, VENDEDOR, CLIENTE, FORNECEDOR]."));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 quando o JSON esta malformado")
    void cadastrarJsonMalformado() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "O corpo da requisicao nao pode ser lido. Verifique o formato do JSON."));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 quando a data de nascimento esta no futuro")
    void cadastrarDataNoFuturo() throws Exception {
        String corpo = """
                { "nome": "Futuro", "perfis": [ "CLIENTE" ],
                  "dataNascimento": "2099-01-01T00:00:00.000+00:00" }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("dataNascimento")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 com caminho indexado nos filhos invalidos")
    void cadastrarFilhosInvalidos() throws Exception {
        String corpo = """
                {
                  "nome": "Invalido",
                  "perfis": [ "CLIENTE" ],
                  "telefones": [ { "ddd": "1", "numero": "abc" } ],
                  "endereco": { "bairro": "Centro" },
                  "documentos": [ { "numero": "" } ],
                  "emails": [ { "endereco": "sem-arroba" } ],
                  "credenciais": [ { "tipo": "USUARIO_SENHA", "nomeUsuario": "x", "senha": "123" } ]
                }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("telefones[0].ddd")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("telefones[0].numero")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("documentos[0].tipo")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("documentos[0].dataEmissao")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("documentos[0].numero")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco.cidade")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco.rua")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("endereco.numero")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("emails[0].endereco")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("credenciais[0].senha")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 409 quando o banco acusa violacao de integridade")
    void cadastrarDocumentoDuplicado() throws Exception {
        when(servico.cadastrar(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        String corpo = """
                { "nome": "Dup", "perfis": [ "CLIENTE" ],
                  "documentos": [ { "tipo": "RG", "numero": "1500", "dataEmissao": "2020-01-01" } ] }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Conflict"));
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 200 com o usuario atualizado")
    void atualizarOk() throws Exception {
        when(servico.atualizar(any(Usuario.class))).thenReturn(usuario(1L, "Nome Novo"));

        String corpo = """
                { "id": 1, "nome": "Nome Novo" }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 400 quando o id nao e informado")
    void atualizarSemId() throws Exception {
        String corpo = """
                { "nome": "Sem Id" }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("id")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 400 quando os perfis vem vazios")
    void atualizarPerfisVazios() throws Exception {
        String corpo = """
                { "id": 1, "perfis": [] }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("perfis")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 400 quando o telefone e enviado sem id")
    void atualizarTelefoneSemId() throws Exception {
        String corpo = """
                { "id": 1, "telefones": [ { "ddd": "11", "numero": "999999999" } ] }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("telefones[0].id")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 404 quando o usuario nao existe")
    void atualizarInexistente() throws Exception {
        when(servico.atualizar(any(Usuario.class)))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        String corpo = """
                { "id": 999, "nome": "Fantasma" }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("DELETE /usuario/excluir/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        doNothing().when(servico).excluir(1L);

        requisicao.perform(delete("/usuario/excluir/1"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(1L);
    }

    @Test
    @DisplayName("DELETE /usuario/excluir/{id}: 404 quando o usuario nao existe")
    void excluirInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Usuario", 999L)).when(servico).excluir(999L);

        requisicao.perform(delete("/usuario/excluir/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuario de id 999 nao foi encontrado"));
    }

    @Test
    @DisplayName("DELETE /usuario/excluir/{id}: 409 quando o usuario tem vendas")
    void excluirComVendas() throws Exception {
        doThrow(new ConflitoException("O usuario de id 1 possui vendas registradas e nao pode ser excluido"))
                .when(servico).excluir(1L);

        requisicao.perform(delete("/usuario/excluir/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("O usuario de id 1 possui vendas registradas e nao pode ser excluido"));
    }

    @Test
    @DisplayName("DELETE /usuario/excluir: 405, o id e obrigatorio na URL")
    void excluirSemIdNaUrl() throws Exception {
        requisicao.perform(delete("/usuario/excluir"))
                .andExpect(status().isMethodNotAllowed());

        verify(servico, never()).excluir(anyLong());
    }

    @Test
    @DisplayName("GET /usuario/{id}: links self, usuarios, telefones, documentos, emails, credenciais, veiculos, mercadorias e vendas")
    void obterPorIdComLinks() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(usuario(1L, "Pedro de Alcantara"));

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/usuario/1"))
                .andExpect(jsonPath("$._links.usuarios.href").value("http://localhost/usuario/usuarios"))
                .andExpect(jsonPath("$._links.telefones.href").value("http://localhost/usuario/1/telefones"))
                .andExpect(jsonPath("$._links.documentos.href").value("http://localhost/usuario/1/documentos"))
                .andExpect(jsonPath("$._links.emails.href").value("http://localhost/usuario/1/emails"))
                .andExpect(jsonPath("$._links.credenciais.href").value("http://localhost/usuario/1/credenciais"))
                .andExpect(jsonPath("$._links.veiculos.href").value("http://localhost/usuario/1/veiculos"))
                .andExpect(jsonPath("$._links.mercadorias.href").value("http://localhost/usuario/1/mercadorias"))
                .andExpect(jsonPath("$._links.vendas.href").value("http://localhost/usuario/1/vendas"));
    }

    @Test
    @DisplayName("GET /usuario/{id}: sem link de endereco quando o usuario nao tem endereco")
    void obterPorIdSemLinkDeEndereco() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(usuario(1L, "Sem Endereco"));

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.endereco").doesNotExist());
    }

    @Test
    @DisplayName("GET /usuario/{id}: com link de endereco quando o usuario tem endereco")
    void obterPorIdComLinkDeEndereco() throws Exception {
        Usuario usuario = usuario(1L, "Com Endereco");
        Endereco endereco = new Endereco();
        endereco.setId(3L);
        endereco.setCidade("Santos");
        usuario.setEndereco(endereco);
        when(servico.obterPorId(1L)).thenReturn(usuario);

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.cidade").value("Santos"))
                .andExpect(jsonPath("$._links.endereco.href").value("http://localhost/usuario/1/endereco"));
    }

    @Test
    @DisplayName("GET /usuario/{id}: sem empresaId e sem link quando o usuario nao tem empresa")
    void obterPorIdSemEmpresa() throws Exception {
        when(servico.obterPorId(1L)).thenReturn(usuario(1L, "Sem Empresa"));

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").doesNotExist())
                .andExpect(jsonPath("$._links.empresa").doesNotExist());
    }

    @Test
    @DisplayName("GET /usuario/{id}: com empresaId e link quando o usuario tem empresa")
    void obterPorIdComEmpresa() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(7L);
        Usuario usuario = usuario(1L, "Com Empresa");
        usuario.setEmpresa(empresa);
        when(servico.obterPorId(1L)).thenReturn(usuario);

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(7))
                .andExpect(jsonPath("$._links.empresa.href").value("http://localhost/empresa/7"));
    }

    @Test
    @DisplayName("GET /usuario/usuarios: cada item mostra o empresaId")
    void listarComEmpresa() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(7L);
        Usuario comEmpresa = usuario(1L, "Com Empresa");
        comEmpresa.setEmpresa(empresa);
        when(servico.listar()).thenReturn(List.of(comEmpresa, usuario(2L, "Sem Empresa")));

        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].empresaId").value(7))
                .andExpect(jsonPath("$[1].empresaId").doesNotExist());
    }

    @Test
    @DisplayName("GET /usuario/usuarios: link self em cada usuario da lista")
    void listarComLinks() throws Exception {
        when(servico.listar()).thenReturn(List.of(usuario(1L, "Primeiro"), usuario(2L, "Segundo")));

        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].links.length()").value(1))
                .andExpect(jsonPath("$[0].links[0].rel").value("self"))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/usuario/1"))
                .andExpect(jsonPath("$[1].links[0].rel").value("self"))
                .andExpect(jsonPath("$[1].links[0].href").value("http://localhost/usuario/2"));
    }

    @Test
    @DisplayName("POST /usuario/cadastro: resposta com link self do usuario criado")
    void cadastrarComLinks() throws Exception {
        when(servico.cadastrar(any(Usuario.class))).thenReturn(usuario(2L, "Maria Silva"));

        String corpo = """
                { "nome": "Maria Silva", "perfis": [ "CLIENTE" ] }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/usuario/2"));
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: resposta com link self do usuario atualizado")
    void atualizarComLinks() throws Exception {
        when(servico.atualizar(any(Usuario.class))).thenReturn(usuario(1L, "Nome Novo"));

        String corpo = """
                { "id": 1, "nome": "Nome Novo" }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/usuario/1"));
    }

    @Test
    @DisplayName("GET /usuario/{id}: resposta de erro nao tem links")
    void erroSemLinks() throws Exception {
        when(servico.obterPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Usuario", 999L));

        requisicao.perform(get("/usuario/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$._links").doesNotExist())
                .andExpect(jsonPath("$.links").doesNotExist());
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 com perfil nulo, sem chegar ao servico")
    void cadastroComPerfilNulo() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Maria\", \"perfis\": [ null ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Os perfis nao podem conter valor nulo"));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 com perfil informado como numero")
    void cadastroComPerfilNumerico() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Maria\", \"perfis\": [ 0 ] }"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /usuario/cadastro: lista enviada como null vale como vazia")
    void cadastroComListasNulas() throws Exception {
        when(servico.cadastrar(any(Usuario.class))).thenReturn(usuario(4L, "Maria"));

        String corpo = """
                { "nome": "Maria", "perfis": [ "CLIENTE" ],
                  "documentos": null, "telefones": null, "emails": null, "credenciais": null }
                """;

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        verify(servico).cadastrar(any(Usuario.class));
    }

    @Test
    @DisplayName("POST /usuario/cadastro: 400 com item nulo em qualquer lista")
    void cadastroComItemNulo() throws Exception {
        for (String lista : List.of("documentos", "telefones", "emails", "credenciais")) {
            requisicao.perform(post("/usuario/cadastro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"nome\": \"Maria\", \"perfis\": [ \"CLIENTE\" ], \"" + lista + "\": [ null ] }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros[0].campo").value(lista + "[0]"));
        }

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 400 com perfil nulo, sem alterar o usuario")
    void atualizacaoComPerfilNulo() throws Exception {
        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"perfis\": [ null ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Os perfis nao podem conter valor nulo"));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: lista enviada como null vale como vazia")
    void atualizacaoComListasNulas() throws Exception {
        when(servico.atualizar(any(Usuario.class))).thenReturn(usuario(1L, "Maria"));

        String corpo = """
                { "id": 1, "documentos": null, "telefones": null, "emails": null }
                """;

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        verify(servico).atualizar(any(Usuario.class));
    }

    @Test
    @DisplayName("PUT /usuario/atualizar: 400 com item nulo em qualquer lista")
    void atualizacaoComItemNulo() throws Exception {
        for (String lista : List.of("documentos", "telefones", "emails")) {
            requisicao.perform(put("/usuario/atualizar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"id\": 1, \"" + lista + "\": [ null ] }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros[0].campo").value(lista + "[0]"));
        }

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
    @DisplayName("GET /usuario/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /usuario/usuarios: 401 sem autenticacao")
    void autorizacaoListarSemAutenticacao() throws Exception {
        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET /usuario/usuarios: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteListaUsuarios() throws Exception {
        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /usuario/{id}: CLIENTE le o proprio cadastro e recebe 200")
    void autorizacaoClienteLeProprioCadastro() throws Exception {
        when(usuarioRepositorio.findById(3L)).thenReturn(Optional.of(usuarioComPerfil(3L, PerfilUsuario.CLIENTE)));
        when(servico.obterPorId(3L)).thenReturn(usuario(3L, "Cliente Proprio"));

        requisicao.perform(get("/usuario/3").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("GET /usuario/{id}: CLIENTE le o cadastro de outro usuario e recebe 403")
    void autorizacaoClienteLeOutroUsuario() throws Exception {
        when(usuarioRepositorio.findById(4L)).thenReturn(Optional.of(usuarioComPerfil(4L, PerfilUsuario.CLIENTE)));

        requisicao.perform(get("/usuario/4").with(logado(3L, PerfilUsuario.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "FORNECEDOR")
    @DisplayName("GET /usuario/{id}: FORNECEDOR recebe 403 sem consultar o repositorio")
    void autorizacaoFornecedorLeUsuario() throws Exception {
        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GET /usuario/{id}: GERENTE le usuario ADMINISTRADOR e recebe 403")
    void autorizacaoGerenteLeAdministrador() throws Exception {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(get("/usuario/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /usuario/cadastro: GERENTE cadastra ADMINISTRADOR e recebe 403")
    void autorizacaoGerenteCadastraAdministrador() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Novo Admin\", \"perfis\": [ \"ADMINISTRADOR\" ] }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /usuario/cadastro: GERENTE cadastra VENDEDOR e recebe 201")
    void autorizacaoGerenteCadastraVendedor() throws Exception {
        when(servico.cadastrar(any(Usuario.class))).thenReturn(usuario(7L, "Novo Vendedor"));

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Novo Vendedor\", \"perfis\": [ \"VENDEDOR\" ] }"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/usuario/7"));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("POST /usuario/cadastro: VENDEDOR cadastra CLIENTE e recebe 201")
    void autorizacaoVendedorCadastraCliente() throws Exception {
        when(servico.cadastrar(any(Usuario.class))).thenReturn(usuario(8L, "Novo Cliente"));

        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Novo Cliente\", \"perfis\": [ \"CLIENTE\" ] }"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/usuario/8"));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("POST /usuario/cadastro: VENDEDOR cadastra GERENTE e recebe 403")
    void autorizacaoVendedorCadastraGerente() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Novo Gerente\", \"perfis\": [ \"GERENTE\" ] }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST /usuario/cadastro: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteCadastraUsuario() throws Exception {
        requisicao.perform(post("/usuario/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Outro Cliente\", \"perfis\": [ \"CLIENTE\" ] }"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /usuario/atualizar: VENDEDOR atualiza cliente e recebe 200")
    void autorizacaoVendedorAtualizaCliente() throws Exception {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.CLIENTE)));
        when(servico.atualizar(any(Usuario.class))).thenReturn(usuario(1L, "Nome Novo"));

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"nome\": \"Nome Novo\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /usuario/atualizar: VENDEDOR atualiza gerente e recebe 403")
    void autorizacaoVendedorAtualizaGerente() throws Exception {
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuarioComPerfil(2L, PerfilUsuario.GERENTE)));

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 2, \"nome\": \"Nome Novo\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /usuario/atualizar: VENDEDOR muda os perfis de cliente para VENDEDOR e recebe 403")
    void autorizacaoVendedorPromoveCliente() throws Exception {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.CLIENTE)));

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"perfis\": [ \"VENDEDOR\" ] }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("PUT /usuario/atualizar: ADMINISTRADOR promove cliente a GERENTE e recebe 200")
    void autorizacaoAdministradorPromoveCliente() throws Exception {
        when(servico.atualizar(any(Usuario.class))).thenReturn(usuario(1L, "Promovido"));

        requisicao.perform(put("/usuario/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"id\": 1, \"perfis\": [ \"GERENTE\" ] }"))
                .andExpect(status().isOk());

        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /usuario/excluir/{id}: GERENTE exclui ADMINISTRADOR e recebe 403")
    void autorizacaoGerenteExcluiAdministrador() throws Exception {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR)));

        requisicao.perform(delete("/usuario/excluir/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /usuario/excluir/{id}: GERENTE exclui VENDEDOR e recebe 204")
    void autorizacaoGerenteExcluiVendedor() throws Exception {
        when(usuarioRepositorio.findById(5L)).thenReturn(Optional.of(usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));

        requisicao.perform(delete("/usuario/excluir/5"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(5L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("DELETE /usuario/excluir/{id}: ADMINISTRADOR exclui ADMINISTRADOR e recebe 204")
    void autorizacaoAdministradorExcluiAdministrador() throws Exception {
        requisicao.perform(delete("/usuario/excluir/1"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(1L);
        verifyNoInteractions(usuarioRepositorio);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("DELETE /usuario/excluir/{id}: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteExcluiUsuario() throws Exception {
        requisicao.perform(delete("/usuario/excluir/3"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /usuario/usuarios: GERENTE recebe so os usuarios que gerencia, incluindo ele mesmo")
    void autorizacaoGerenteListaSoOsGerenciaveis() throws Exception {
        when(servico.listar()).thenReturn(List.of(
                usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR),
                usuarioComPerfil(2L, PerfilUsuario.GERENTE),
                usuarioComPerfil(3L, PerfilUsuario.VENDEDOR),
                usuarioComPerfil(4L, PerfilUsuario.CLIENTE),
                usuarioComPerfil(5L, PerfilUsuario.FORNECEDOR)));

        requisicao.perform(get("/usuario/usuarios").with(logado(2L, PerfilUsuario.GERENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(2, 3, 4)));
    }

    @Test
    @DisplayName("GET /usuario/usuarios: VENDEDOR recebe so os clientes e ele mesmo")
    void autorizacaoVendedorListaClientesEEleMesmo() throws Exception {
        when(servico.listar()).thenReturn(List.of(
                usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR),
                usuarioComPerfil(2L, PerfilUsuario.GERENTE),
                usuarioComPerfil(3L, PerfilUsuario.VENDEDOR),
                usuarioComPerfil(4L, PerfilUsuario.CLIENTE),
                usuarioComPerfil(5L, PerfilUsuario.VENDEDOR)));

        requisicao.perform(get("/usuario/usuarios").with(logado(3L, PerfilUsuario.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(3, 4)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("GET /usuario/usuarios: ADMINISTRADOR recebe a lista completa")
    void autorizacaoAdministradorListaTodos() throws Exception {
        when(servico.listar()).thenReturn(List.of(
                usuarioComPerfil(1L, PerfilUsuario.ADMINISTRADOR),
                usuarioComPerfil(5L, PerfilUsuario.FORNECEDOR)));

        requisicao.perform(get("/usuario/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
