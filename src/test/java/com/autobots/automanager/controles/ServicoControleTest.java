package com.autobots.automanager.controles;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import java.math.BigDecimal;
import java.util.List;

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
import com.autobots.automanager.conversores.ServicoConversor;
import com.autobots.automanager.entidades.Servico;
import com.autobots.automanager.excecoes.ConflitoException;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.modelo.AdicionadorLinkServico;
import com.autobots.automanager.servicos.ServicoServico;

@WebMvcTest(ServicoControle.class)
@Import({ ServicoConversor.class, AdicionadorLinkServico.class, SegurancaTesteConfiguracao.class })
@WithMockUser(roles = "ADMINISTRADOR")
class ServicoControleTest {

    @Autowired
    private MockMvc requisicao;

    @MockBean
    private ServicoServico servico;

    private Servico novoServico(Long id) {
        Servico item = new Servico();
        item.setId(id);
        item.setNome("Alinhamento");
        item.setValor(new BigDecimal("50.00"));
        return item;
    }

    @Test
    @DisplayName("GET /empresa/{id}/servicos: 200 com a lista e link self em cada item")
    void listarOk() throws Exception {
        when(servico.listarPorEmpresa(1L)).thenReturn(List.of(novoServico(20L)));

        requisicao.perform(get("/empresa/1/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Alinhamento"))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/servico/20"));
    }

    @Test
    @DisplayName("POST /empresa/{id}/servicos: 201 com header Location e link self")
    void criarOk() throws Exception {
        when(servico.criar(anyLong(), any(Servico.class))).thenReturn(novoServico(20L));

        String corpo = """
                { "nome": "Alinhamento", "valor": 50.00 }
                """;

        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/servico/20"))
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.valor").value(50.0))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/servico/20"));
    }

    @Test
    @DisplayName("POST /empresa/{id}/servicos: 400 quando faltam os campos obrigatorios")
    void criarSemCampos() throws Exception {
        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("valor")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("POST /empresa/{id}/servicos: 400 quando o valor e negativo")
    void criarValorNegativo() throws Exception {
        String corpo = """
                { "nome": "Alinhamento", "valor": -1 }
                """;

        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("O valor nao pode ser negativo"));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("GET /servico/{id}: 200 com o servico")
    void obterOk() throws Exception {
        when(servico.obterPorId(20L)).thenReturn(novoServico(20L));

        requisicao.perform(get("/servico/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alinhamento"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/servico/20"));
    }

    @Test
    @DisplayName("GET /servico/{id}: 404 quando nao existe, sem links")
    void obterInexistente() throws Exception {
        when(servico.obterPorId(999L)).thenThrow(new RecursoNaoEncontradoException("Servico", 999L));

        requisicao.perform(get("/servico/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$._links").doesNotExist());
    }

    @Test
    @DisplayName("PUT /servico/{id}: 200 com o servico atualizado")
    void atualizarOk() throws Exception {
        when(servico.atualizar(anyLong(), any(Servico.class))).thenReturn(novoServico(20L));

        requisicao.perform(put("/servico/20")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"valor\": 50.00 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(50.0));
    }

    @Test
    @DisplayName("PUT /servico/{id}: 400 quando o valor tem mais de 2 decimais")
    void atualizarValorInvalido() throws Exception {
        requisicao.perform(put("/servico/20")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"valor\": 1.234 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("valor")));

        verifyNoInteractions(servico);
    }

    @Test
    @DisplayName("DELETE /servico/{id}: 204 sem corpo")
    void excluirOk() throws Exception {
        requisicao.perform(delete("/servico/20"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(20L);
    }

    @Test
    @DisplayName("DELETE /servico/{id}: 409 quando o servico possui vendas")
    void excluirComVendas() throws Exception {
        doThrow(new ConflitoException("O servico de id 20 possui vendas registradas e nao pode ser excluido"))
                .when(servico).excluir(20L);

        requisicao.perform(delete("/servico/20"))
                .andExpect(status().isConflict());
    }

    private static final String CORPO_CADASTRO = """
            { "nome": "Alinhamento", "valor": 50.00 }
            """;

    @Test
    @WithAnonymousUser
    @DisplayName("GET /servico/{id}: 401 sem autenticacao")
    void autorizacaoObterSemAutenticacao() throws Exception {
        requisicao.perform(get("/servico/20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.NAO_AUTENTICADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /empresa/{id}/servicos: 401 sem autenticacao")
    void autorizacaoCadastrarSemAutenticacao() throws Exception {
        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("GET /servico/{id}: VENDEDOR le e recebe 200")
    void autorizacaoVendedorLeServico() throws Exception {
        when(servico.obterPorId(20L)).thenReturn(novoServico(20L));

        requisicao.perform(get("/servico/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("GET /empresa/{id}/servicos: VENDEDOR lista e recebe 200")
    void autorizacaoVendedorListaServicos() throws Exception {
        when(servico.listarPorEmpresa(1L)).thenReturn(List.of(novoServico(20L)));

        requisicao.perform(get("/empresa/1/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("POST /empresa/{id}/servicos: VENDEDOR recebe 403 sem chamar o servico")
    void autorizacaoVendedorCadastra() throws Exception {
        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("PUT /servico/{id}: VENDEDOR recebe 403 sem chamar o servico")
    void autorizacaoVendedorAltera() throws Exception {
        requisicao.perform(put("/servico/20")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"valor\": 50.00 }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    @DisplayName("DELETE /servico/{id}: VENDEDOR recebe 403 sem chamar o servico")
    void autorizacaoVendedorExclui() throws Exception {
        requisicao.perform(delete("/servico/20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("POST /empresa/{id}/servicos: GERENTE cadastra e recebe 201")
    void autorizacaoGerenteCadastra() throws Exception {
        when(servico.criar(anyLong(), any(Servico.class))).thenReturn(novoServico(20L));

        requisicao.perform(post("/empresa/1/servicos")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/servico/20"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("PUT /servico/{id}: GERENTE altera e recebe 200")
    void autorizacaoGerenteAltera() throws Exception {
        when(servico.atualizar(anyLong(), any(Servico.class))).thenReturn(novoServico(20L));

        requisicao.perform(put("/servico/20")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"valor\": 50.00 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(50.0));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("DELETE /servico/{id}: GERENTE exclui e recebe 204")
    void autorizacaoGerenteExclui() throws Exception {
        requisicao.perform(delete("/servico/20"))
                .andExpect(status().isNoContent());

        verify(servico).excluir(20L);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET /servico/{id}: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteLeServico() throws Exception {
        requisicao.perform(get("/servico/20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaSeguranca.ACESSO_NEGADO));

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET /empresa/{id}/servicos: CLIENTE recebe 403 sem chamar o servico")
    void autorizacaoClienteListaServicos() throws Exception {
        requisicao.perform(get("/empresa/1/servicos"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }

    @Test
    @WithMockUser(roles = "FORNECEDOR")
    @DisplayName("GET /servico/{id}: FORNECEDOR recebe 403 sem chamar o servico")
    void autorizacaoFornecedorLeServico() throws Exception {
        requisicao.perform(get("/servico/20"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(servico);
    }
}
