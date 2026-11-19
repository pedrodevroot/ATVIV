package com.autobots.automanager;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.jwt.ProvedorJwt;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:autobots-seguranca;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class IntegracaoSegurancaTest {

    private static final String SENHA = "123456";
    private static final String MENSAGEM_LOGIN = "Nome de usuario ou senha invalidos, ou credencial inativa";
    private static final String OUTRO_SEGREDO =
            "outro-segredo-totalmente-diferente-do-segredo-da-aplicacao-com-mais-de-sessenta-e-quatro-caracteres";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CredencialRepositorio credencialRepositorio;
    @Autowired
    private PasswordEncoder codificador;
    @Value("${jwt.secret}")
    private String segredo;

    private ResultActions login(String nomeUsuario, String senha) throws Exception {
        return mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nomeUsuario\": \"" + nomeUsuario + "\", \"senha\": \"" + senha + "\" }"));
    }

    private String token(String nomeUsuario) throws Exception {
        return login(nomeUsuario, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }

    private String admin() throws Exception {
        return token("dompedroadmin");
    }

    private String gerente() throws Exception {
        return token("dompedrogerente");
    }

    private String vendedor() throws Exception {
        return token("dompedrovendedor");
    }

    private String cliente() throws Exception {
        return token("dompedrocliente");
    }

    private ResultActions comToken(String token, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions obter(String token, String url) throws Exception {
        return comToken(token, get(url));
    }

    private ResultActions enviarPost(String token, String url, String corpo) throws Exception {
        return comToken(token, post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions enviarPut(String token, String url, String corpo) throws Exception {
        return comToken(token, put(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions remover(String token, String url) throws Exception {
        return comToken(token, delete(url));
    }

    private long idDe(ResultActions resultado) throws Exception {
        String corpo = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarComo(String token, String url, String corpo) throws Exception {
        return idDe(enviarPost(token, url, corpo).andExpect(status().isCreated()));
    }

    private String unico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID();
    }

    private String usuarioJson(String perfil) {
        return "{ \"nome\": \"Pessoa de Teste\", \"perfis\": [ \"" + perfil + "\" ] }";
    }

    private String usuarioComCredencialJson(String perfil, String nomeUsuario, String senha) {
        return "{ \"nome\": \"Pessoa de Teste\", \"perfis\": [ \"" + perfil + "\" ], \"credenciais\": [ "
                + "{ \"tipo\": \"USUARIO_SENHA\", \"nomeUsuario\": \"" + nomeUsuario + "\", \"senha\": \""
                + senha + "\" } ] }";
    }

    private String vendaJson(long clienteId, long funcionarioId) {
        return "{ \"identificacao\": \"" + unico("venda") + "\", \"clienteId\": " + clienteId
                + ", \"funcionarioId\": " + funcionarioId + ", \"servicoIds\": [ 1 ] }";
    }

    private void negado(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));
    }

    @Test
    @DisplayName("autenticacao: login dos quatro usuarios da carga devolve o token no header e no corpo")
    void autenticacaoLoginDosUsuariosDaCarga() throws Exception {
        String[][] usuarios = {
                { "dompedrovendedor", "1" },
                { "dompedrocliente", "3" },
                { "dompedroadmin", "4" },
                { "dompedrogerente", "5" } };
        for (String[] usuario : usuarios) {
            ResultActions resposta = login(usuario[0], SENHA)
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.usuarioId").value(Integer.parseInt(usuario[1])))
                    .andExpect(jsonPath("$._links.usuario.href", endsWith("/usuario/" + usuario[1])));
            String cabecalho = resposta.andReturn().getResponse().getHeader(HttpHeaders.AUTHORIZATION);
            String corpo = resposta.andReturn().getResponse().getContentAsString();
            assertEquals(cabecalho, "Bearer " + JsonPath.read(corpo, "$.token"));
        }
    }

    @Test
    @DisplayName("autenticacao: senha errada responde 401 com mensagem generica")
    void autenticacaoSenhaErrada() throws Exception {
        login("dompedroadmin", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(MENSAGEM_LOGIN));
    }

    @Test
    @DisplayName("autenticacao: usuario inexistente responde 401 com a mesma mensagem generica")
    void autenticacaoUsuarioInexistente() throws Exception {
        login("ninguem-com-esse-nome", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(MENSAGEM_LOGIN));
    }

    @Test
    @DisplayName("autenticacao: corpo sem senha responde 400 com a lista de erros")
    void autenticacaoCorpoSemSenha() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nomeUsuario\": \"dompedroadmin\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isNotEmpty());
    }

    @Test
    @DisplayName("autenticacao: rota protegida sem header responde 401 em JSON com WWW-Authenticate")
    void autenticacaoSemHeader() throws Exception {
        mvc.perform(get("/usuario/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO))
                .andExpect(jsonPath("$.caminho").value("/usuario/usuarios"));
    }

    @Test
    @DisplayName("autenticacao: rota inexistente sem token responde 401")
    void autenticacaoRotaInexistenteSemToken() throws Exception {
        mvc.perform(get("/rota-que-nao-existe")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: header com token malformado responde 401")
    void autenticacaoTokenMalformado() throws Exception {
        comToken("Bearer abc", get("/usuario/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: header com esquema Basic responde 401")
    void autenticacaoEsquemaBasic() throws Exception {
        comToken("Basic ZG9tcGVkcm9hZG1pbjoxMjM0NTY=", get("/usuario/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: token expirado responde 401")
    void autenticacaoTokenExpirado() throws Exception {
        String expirado = new ProvedorJwt(segredo, -1000).proverJwt("dompedroadmin");
        comToken("Bearer " + expirado, get("/usuario/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: token assinado com outro segredo responde 401")
    void autenticacaoTokenDeOutroSegredo() throws Exception {
        String forjado = new ProvedorJwt(OUTRO_SEGREDO, 60000).proverJwt("dompedroadmin");
        comToken("Bearer " + forjado, get("/usuario/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: credencial inativada bloqueia o login e invalida o token emitido antes")
    void autenticacaoCredencialInativada() throws Exception {
        String administrador = admin();
        String nomeUsuario = unico("inativa");
        String corpo = enviarPost(administrador, "/usuario/cadastro",
                usuarioComCredencialJson("CLIENTE", nomeUsuario, "segredo1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long usuarioId = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        long credencialId = ((Number) JsonPath.read(corpo, "$.credenciais[0].id")).longValue();

        String emitido = login(nomeUsuario, "segredo1").andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        obter(emitido, "/usuario/" + usuarioId).andExpect(status().isOk());

        enviarPut(administrador, "/credencial/" + credencialId, "{ \"inativo\": true }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inativo").value(true));

        login(nomeUsuario, "segredo1").andExpect(status().isUnauthorized());
        obter(emitido, "/usuario/" + usuarioId).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticacao: o login preenche o ultimo acesso da credencial")
    void autenticacaoLoginPreencheUltimoAcesso() throws Exception {
        String administrador = admin();
        String nomeUsuario = unico("acesso");
        String corpo = enviarPost(administrador, "/usuario/cadastro",
                usuarioComCredencialJson("CLIENTE", nomeUsuario, "segredo1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long credencialId = ((Number) JsonPath.read(corpo, "$.credenciais[0].id")).longValue();

        obter(administrador, "/credencial/" + credencialId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoAcesso").doesNotExist());

        login(nomeUsuario, "segredo1").andExpect(status().isOk());

        obter(administrador, "/credencial/" + credencialId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoAcesso").exists());
    }

    @Test
    @DisplayName("autenticacao: a senha e gravada com BCrypt")
    void autenticacaoSenhaGravadaComBcrypt() {
        CredencialUsuarioSenha credencial = credencialRepositorio.buscarPorNomeUsuario("dompedroadmin")
                .orElseThrow();
        assertNotEquals(SENHA, credencial.getSenha());
        assertTrue(credencial.getSenha().startsWith("$2"));
        assertTrue(codificador.matches(SENHA, credencial.getSenha()));
    }

    @Test
    @DisplayName("autenticacao: senha alterada pela API vale no login e a antiga deixa de valer")
    void autenticacaoSenhaAlterada() throws Exception {
        String administrador = admin();
        String nomeUsuario = unico("troca");
        String corpo = enviarPost(administrador, "/usuario/cadastro",
                usuarioComCredencialJson("CLIENTE", nomeUsuario, "segredo1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long credencialId = ((Number) JsonPath.read(corpo, "$.credenciais[0].id")).longValue();

        enviarPut(administrador, "/credencial/" + credencialId, "{ \"senha\": \"novaSenha1\" }")
                .andExpect(status().isOk());

        login(nomeUsuario, "novaSenha1").andExpect(status().isOk());
        login(nomeUsuario, "segredo1").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("administrador: cria e exclui outro administrador")
    void administradorCriaEExcluiAdministrador() throws Exception {
        String administrador = admin();
        long id = criarComo(administrador, "/usuario/cadastro", usuarioJson("ADMINISTRADOR"));
        remover(administrador, "/usuario/excluir/" + id).andExpect(status().isNoContent());
        obter(administrador, "/usuario/" + id).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("administrador: lista e cadastra empresas")
    void administradorEmpresas() throws Exception {
        String administrador = admin();
        obter(administrador, "/empresa/empresas").andExpect(status().isOk());
        criarComo(administrador, "/empresa/cadastro", "{ \"razaoSocial\": \"Empresa do administrador\" }");
    }

    @Test
    @DisplayName("administrador: lista todos os usuarios da carga")
    void administradorListaUsuarios() throws Exception {
        obter(admin(), "/usuario/usuarios")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(1, 2, 3, 4, 5)));
    }

    @Test
    @DisplayName("gerente: cria gerente, vendedor e cliente")
    void gerenteCriaPerfisGerenciaveis() throws Exception {
        String token = gerente();
        criarComo(token, "/usuario/cadastro", usuarioJson("GERENTE"));
        criarComo(token, "/usuario/cadastro", usuarioJson("VENDEDOR"));
        criarComo(token, "/usuario/cadastro", usuarioJson("CLIENTE"));
    }

    @Test
    @DisplayName("gerente: nao cria administrador nem fornecedor")
    void gerenteNaoCriaAdministradorNemFornecedor() throws Exception {
        String token = gerente();
        negado(enviarPost(token, "/usuario/cadastro", usuarioJson("ADMINISTRADOR")));
        negado(enviarPost(token, "/usuario/cadastro", usuarioJson("FORNECEDOR")));
    }

    @Test
    @DisplayName("gerente: nao le, altera nem exclui o administrador")
    void gerenteNaoMexeNoAdministrador() throws Exception {
        String token = gerente();
        negado(obter(token, "/usuario/4"));
        negado(remover(token, "/usuario/excluir/4"));
        negado(enviarPut(token, "/usuario/atualizar", "{ \"id\": 4, \"nomeSocial\": \"Alterado\" }"));
    }

    @Test
    @DisplayName("gerente: gerencia credencial de cliente, mas nao a de administrador nem a de fornecedor")
    void gerenteGerenciaCredencialSoDeQuemGerencia() throws Exception {
        String token = gerente();
        String nomeUsuario = unico("cliente-gerente");
        long credencialAdministrador = credencialRepositorio.buscarPorNomeUsuario("dompedroadmin").orElseThrow().getId();

        long credencial = criarComo(token, "/usuario/3/credenciais", "{ \"tipo\": \"USUARIO_SENHA\", \"nomeUsuario\": \""
                + nomeUsuario + "\", \"senha\": \"segredo1\" }");
        enviarPut(token, "/credencial/" + credencial, "{ \"senha\": \"novaSenha1\" }").andExpect(status().isOk());
        login(nomeUsuario, "novaSenha1").andExpect(status().isOk());
        remover(token, "/credencial/" + credencial).andExpect(status().isNoContent());

        negado(enviarPut(token, "/credencial/" + credencialAdministrador, "{ \"senha\": \"trocada1\" }"));
        negado(remover(token, "/credencial/" + credencialAdministrador));
        negado(enviarPost(token, "/usuario/4/credenciais", "{ \"tipo\": \"CODIGO_BARRA\", \"codigo\": 123456 }"));
        negado(enviarPost(token, "/usuario/2/credenciais", "{ \"tipo\": \"CODIGO_BARRA\", \"codigo\": 123456 }"));
        login("dompedroadmin", SENHA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("gerente: a lista de usuarios nao traz o administrador nem o fornecedor")
    void gerenteListaFiltrada() throws Exception {
        obter(gerente(), "/usuario/usuarios")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(1, 3, 5)))
                .andExpect(jsonPath("$[*].id", not(hasItem(4))))
                .andExpect(jsonPath("$[*].id", not(hasItem(2))));
    }

    @Test
    @DisplayName("gerente: faz o CRUD de mercadoria da empresa")
    void gerenteCrudDeMercadoria() throws Exception {
        String token = gerente();
        long id = criarComo(token, "/empresa/1/mercadorias", """
                { "nome": "Filtro do gerente", "fabricacao": "2025-01-15T00:00:00.000+00:00",
                  "quantidade": 5, "valor": 40.00 }
                """);
        obter(token, "/mercadoria/" + id).andExpect(status().isOk());
        enviarPut(token, "/mercadoria/" + id, "{ \"quantidade\": 7 }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(7));
        remover(token, "/mercadoria/" + id).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("gerente: faz o CRUD de servico da empresa")
    void gerenteCrudDeServico() throws Exception {
        String token = gerente();
        long id = criarComo(token, "/empresa/1/servicos", "{ \"nome\": \"Revisao do gerente\", \"valor\": 80.00 }");
        obter(token, "/servico/" + id).andExpect(status().isOk());
        enviarPut(token, "/servico/" + id, "{ \"valor\": 90.00 }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(90.0));
        remover(token, "/servico/" + id).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("gerente: faz o CRUD de venda da empresa com qualquer funcionario")
    void gerenteCrudDeVenda() throws Exception {
        String token = gerente();
        long id = criarComo(token, "/empresa/1/vendas", vendaJson(3, 1));
        enviarPut(token, "/venda/" + id, "{ \"identificacao\": \"" + unico("venda-alterada") + "\" }")
                .andExpect(status().isOk());
        remover(token, "/venda/" + id).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("gerente: nao acessa empresas")
    void gerenteNaoAcessaEmpresas() throws Exception {
        String token = gerente();
        negado(obter(token, "/empresa/empresas"));
        negado(enviarPost(token, "/empresa/1/telefones", "{ \"ddd\": \"11\", \"numero\": \"987654321\" }"));
    }

    @Test
    @DisplayName("vendedor: faz o CRUD de cliente")
    void vendedorCrudDeCliente() throws Exception {
        String token = vendedor();
        long id = criarComo(token, "/usuario/cadastro", usuarioJson("CLIENTE"));
        enviarPut(token, "/usuario/atualizar", "{ \"id\": " + id + ", \"nomeSocial\": \"Atualizado\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeSocial").value("Atualizado"));
        remover(token, "/usuario/excluir/" + id).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("vendedor: nao cria vendedor, nao altera o gerente e nao promove cliente")
    void vendedorNaoGerenciaOutrosPerfis() throws Exception {
        String token = vendedor();
        negado(enviarPost(token, "/usuario/cadastro", usuarioJson("VENDEDOR")));
        negado(enviarPut(token, "/usuario/atualizar", "{ \"id\": 5, \"nomeSocial\": \"Alterado\" }"));
        long cliente = criarComo(token, "/usuario/cadastro", usuarioJson("CLIENTE"));
        negado(enviarPut(token, "/usuario/atualizar",
                "{ \"id\": " + cliente + ", \"perfis\": [ \"VENDEDOR\" ] }"));
    }

    @Test
    @DisplayName("vendedor: nao cria, altera nem exclui credencial de usuario ja existente")
    void vendedorNaoMexeEmCredencialDeUsuarioExistente() throws Exception {
        String administrador = admin();
        long vendaDoCliente = criarComo(administrador, "/empresa/1/vendas", vendaJson(3, 5));
        long credencialCliente = credencialRepositorio.buscarPorNomeUsuario("dompedrocliente").orElseThrow().getId();
        long credencialVendedor = credencialRepositorio.buscarPorNomeUsuario("dompedrovendedor").orElseThrow().getId();
        String nomeNovo = unico("invasor");
        String nomeAtualizacao = unico("atualizacao");
        String token = vendedor();

        negado(enviarPut(token, "/credencial/" + credencialCliente, "{ \"senha\": \"trocada1\" }"));
        negado(enviarPut(token, "/credencial/" + credencialCliente, "{ \"inativo\": true }"));
        negado(enviarPost(token, "/usuario/3/credenciais", "{ \"tipo\": \"USUARIO_SENHA\", \"nomeUsuario\": \""
                + nomeNovo + "\", \"senha\": \"segredo1\" }"));
        negado(remover(token, "/credencial/" + credencialCliente));
        negado(enviarPut(token, "/credencial/" + credencialVendedor, "{ \"senha\": \"trocada1\" }"));
        negado(enviarPost(token, "/usuario/1/credenciais", "{ \"tipo\": \"CODIGO_BARRA\", \"codigo\": 123456 }"));
        enviarPut(token, "/usuario/atualizar", "{ \"id\": 3, \"nomeSocial\": \"Cliente\", \"credenciais\": [ "
                + "{ \"tipo\": \"USUARIO_SENHA\", \"nomeUsuario\": \"" + nomeAtualizacao
                + "\", \"senha\": \"segredo1\" } ] }").andExpect(status().isOk());

        login("dompedrocliente", "trocada1").andExpect(status().isUnauthorized());
        login(nomeNovo, "segredo1").andExpect(status().isUnauthorized());
        login(nomeAtualizacao, "segredo1").andExpect(status().isUnauthorized());
        obter(cliente(), "/venda/" + vendaDoCliente).andExpect(status().isOk());
        negado(obter(token, "/venda/" + vendaDoCliente));
    }

    @Test
    @DisplayName("vendedor: cadastra cliente novo com credencial e ele faz login")
    void vendedorCadastraClienteNovoComCredencial() throws Exception {
        String nomeUsuario = unico("novo-cliente");

        long id = criarComo(vendedor(), "/usuario/cadastro", usuarioComCredencialJson("CLIENTE", nomeUsuario, "segredo1"));

        String tokenCliente = login(nomeUsuario, "segredo1")
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        obter(tokenCliente, "/usuario/" + id).andExpect(status().isOk());
    }

    @Test
    @DisplayName("vendedor: le mercadorias e servicos")
    void vendedorLeMercadoriasEServicos() throws Exception {
        String token = vendedor();
        obter(token, "/mercadoria/1").andExpect(status().isOk());
        obter(token, "/empresa/1/mercadorias").andExpect(status().isOk());
        obter(token, "/servico/1").andExpect(status().isOk());
        obter(token, "/empresa/1/servicos").andExpect(status().isOk());
    }

    @Test
    @DisplayName("vendedor: nao escreve em mercadorias e servicos")
    void vendedorNaoEscreveEmMercadoriasEServicos() throws Exception {
        String token = vendedor();
        negado(enviarPost(token, "/empresa/1/mercadorias", """
                { "nome": "Filtro", "fabricacao": "2025-01-15T00:00:00.000+00:00",
                  "quantidade": 5, "valor": 40.00 }
                """));
        negado(enviarPut(token, "/servico/1", "{ \"valor\": 99.00 }"));
        negado(remover(token, "/mercadoria/1"));
    }

    @Test
    @DisplayName("vendedor: cria venda feita por si mesmo")
    void vendedorCriaVendaPropria() throws Exception {
        criarComo(vendedor(), "/empresa/1/vendas", vendaJson(3, 1));
    }

    @Test
    @DisplayName("vendedor: nao cria venda em nome de outro funcionario")
    void vendedorNaoCriaVendaDeOutro() throws Exception {
        negado(enviarPost(vendedor(), "/empresa/1/vendas", vendaJson(3, 5)));
    }

    @Test
    @DisplayName("vendedor: le a venda da carga feita por ele")
    void vendedorLeVendaPropria() throws Exception {
        obter(vendedor(), "/venda/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioId").value(1));
    }

    @Test
    @DisplayName("vendedor: nao le venda feita por outro funcionario")
    void vendedorNaoLeVendaDeOutro() throws Exception {
        long venda = criarComo(admin(), "/empresa/1/vendas", vendaJson(3, 5));
        negado(obter(vendedor(), "/venda/" + venda));
    }

    @Test
    @DisplayName("vendedor: nao altera nem exclui venda")
    void vendedorNaoAlteraNemExcluiVenda() throws Exception {
        String token = vendedor();
        negado(enviarPut(token, "/venda/1", "{ \"identificacao\": \"" + unico("venda") + "\" }"));
        negado(remover(token, "/venda/1"));
    }

    @Test
    @DisplayName("vendedor: a lista de vendas da empresa traz so as dele")
    void vendedorListaDeVendasFiltrada() throws Exception {
        criarComo(admin(), "/empresa/1/vendas", vendaJson(3, 5));
        obter(vendedor(), "/empresa/1/vendas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].funcionarioId", everyItem(is(1))));
    }

    @Test
    @DisplayName("vendedor: le o proprio cadastro")
    void vendedorLeOProprioCadastro() throws Exception {
        obter(vendedor(), "/usuario/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("cliente: le o proprio cadastro e os sub-recursos")
    void clienteLeOProprioCadastro() throws Exception {
        String token = cliente();
        obter(token, "/usuario/3").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        obter(token, "/usuario/3/veiculos").andExpect(status().isOk());
        obter(token, "/usuario/3/emails").andExpect(status().isOk());
    }

    @Test
    @DisplayName("cliente: nao le o cadastro de outro usuario nem a lista de usuarios")
    void clienteNaoLeOutrosUsuarios() throws Exception {
        String token = cliente();
        negado(obter(token, "/usuario/1"));
        negado(obter(token, "/usuario/usuarios"));
    }

    @Test
    @DisplayName("cliente: le a venda em que foi consumidor")
    void clienteLeVendaPropria() throws Exception {
        obter(cliente(), "/venda/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(3));
    }

    @Test
    @DisplayName("cliente: nao le venda de outro cliente")
    void clienteNaoLeVendaDeOutroCliente() throws Exception {
        String administrador = admin();
        long outroCliente = criarComo(administrador, "/usuario/cadastro", usuarioJson("CLIENTE"));
        long venda = criarComo(administrador, "/empresa/1/vendas", vendaJson(outroCliente, 1));
        negado(obter(cliente(), "/venda/" + venda));
    }

    @Test
    @DisplayName("cliente: as listas de vendas trazem so as dele")
    void clienteListasDeVendasFiltradas() throws Exception {
        String administrador = admin();
        long outroCliente = criarComo(administrador, "/usuario/cadastro", usuarioJson("CLIENTE"));
        criarComo(administrador, "/empresa/1/vendas", vendaJson(outroCliente, 1));
        String token = cliente();
        obter(token, "/usuario/3/vendas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].clienteId", everyItem(is(3))));
        obter(token, "/empresa/1/vendas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].clienteId", everyItem(is(3))));
    }

    @Test
    @DisplayName("cliente: nao le mercadorias nem servicos")
    void clienteNaoLeMercadoriasNemServicos() throws Exception {
        String token = cliente();
        negado(obter(token, "/mercadoria/1"));
        negado(obter(token, "/servico/1"));
    }

    @Test
    @DisplayName("cliente: nao cadastra usuario, nao se atualiza e nao cria venda")
    void clienteNaoEscreve() throws Exception {
        String token = cliente();
        negado(enviarPost(token, "/usuario/cadastro", usuarioJson("CLIENTE")));
        negado(enviarPut(token, "/usuario/atualizar", "{ \"id\": 3, \"nomeSocial\": \"Alterado\" }"));
        negado(enviarPost(token, "/empresa/1/vendas", vendaJson(3, 1)));
    }

    @Test
    @DisplayName("fornecedor: a credencial antiga do fornecedor nao existe mais")
    void fornecedorCredencialAntigaNaoExiste() throws Exception {
        login("dompedrofornecedor", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("fornecedor: credencial criada para o fornecedor nao autentica")
    void fornecedorCredencialDoFornecedorNaoAutentica() throws Exception {
        String administrador = admin();
        String nomeUsuario = unico("fornecedor");
        criarComo(administrador, "/usuario/2/credenciais", "{ \"tipo\": \"USUARIO_SENHA\", \"nomeUsuario\": \""
                + nomeUsuario + "\", \"senha\": \"segredo1\" }");
        login(nomeUsuario, "segredo1").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("fornecedor: o administrador le o fornecedor e as mercadorias dele")
    void fornecedorAdministradorLeOFornecedor() throws Exception {
        String administrador = admin();
        obter(administrador, "/usuario/2").andExpect(status().isOk());
        obter(administrador, "/usuario/2/mercadorias").andExpect(status().isOk());
    }
}
