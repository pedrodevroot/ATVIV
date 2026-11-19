package com.autobots.automanager.controles;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.autobots.automanager.configuracao.Autorizacoes;
import com.autobots.automanager.conversores.VendaConversor;
import com.autobots.automanager.dtos.VendaAtualizacaoDto;
import com.autobots.automanager.dtos.VendaCadastroDto;
import com.autobots.automanager.dtos.VendaRespostaDto;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.modelo.AdicionadorLinkVenda;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.VendaServico;

@RestController
public class VendaControle {

    @Autowired
    private VendaServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private VendaConversor conversor;
    @Autowired
    private AdicionadorLinkVenda adicionadorLink;

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/empresa/{empresaId}/vendas")
    public ResponseEntity<List<VendaRespostaDto>> obterVendas(@PathVariable long empresaId) {
        List<VendaRespostaDto> resposta = conversor
                .paraResposta(autorizacao.filtrarVendas(servico.listarPorEmpresa(empresaId)));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/usuario/{usuarioId}/vendas")
    public ResponseEntity<List<VendaRespostaDto>> obterVendasDoUsuario(@PathVariable long usuarioId) {
        List<VendaRespostaDto> resposta = conversor
                .paraResposta(autorizacao.filtrarVendas(servico.listarPorUsuario(usuarioId)));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/veiculo/{veiculoId}/vendas")
    public ResponseEntity<List<VendaRespostaDto>> obterVendasDoVeiculo(@PathVariable long veiculoId) {
        List<VendaRespostaDto> resposta = conversor
                .paraResposta(autorizacao.filtrarVendas(servico.listarPorVeiculo(veiculoId)));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PostMapping("/empresa/{empresaId}/vendas")
    public ResponseEntity<VendaRespostaDto> cadastrarVenda(@PathVariable long empresaId,
            @Valid @RequestBody VendaCadastroDto dto) {
        autorizacao.verificarCriacaoVenda(dto.getFuncionarioId());
        Venda criada = servico.criar(empresaId, dto);
        VendaRespostaDto resposta = conversor.paraResposta(criada);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/venda/" + criada.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/venda/{id}")
    public ResponseEntity<VendaRespostaDto> obterVenda(@PathVariable long id) {
        autorizacao.verificarLeituraVenda(id);
        VendaRespostaDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR_GERENTE)
    @PutMapping("/venda/{id}")
    public ResponseEntity<VendaRespostaDto> atualizarVenda(@PathVariable long id,
            @Valid @RequestBody VendaAtualizacaoDto dto) {
        VendaRespostaDto resposta = conversor.paraResposta(servico.atualizar(id, dto));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR_GERENTE)
    @DeleteMapping("/venda/{id}")
    public ResponseEntity<Void> excluirVenda(@PathVariable long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
