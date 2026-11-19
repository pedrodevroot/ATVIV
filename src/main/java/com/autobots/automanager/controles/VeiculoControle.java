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
import com.autobots.automanager.conversores.VeiculoConversor;
import com.autobots.automanager.dtos.VeiculoAtualizacaoDto;
import com.autobots.automanager.dtos.VeiculoCadastroDto;
import com.autobots.automanager.dtos.VeiculoDto;
import com.autobots.automanager.entidades.Veiculo;
import com.autobots.automanager.modelo.AdicionadorLinkVeiculo;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.VeiculoServico;

@RestController
public class VeiculoControle {

    @Autowired
    private VeiculoServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private VeiculoConversor conversor;
    @Autowired
    private AdicionadorLinkVeiculo adicionadorLink;

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/usuario/{usuarioId}/veiculos")
    public ResponseEntity<List<VeiculoDto>> obterVeiculos(@PathVariable long usuarioId) {
        autorizacao.verificarLeituraUsuario(usuarioId);
        List<VeiculoDto> resposta = conversor.paraResposta(servico.listarPorUsuario(usuarioId));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PostMapping("/usuario/{usuarioId}/veiculos")
    public ResponseEntity<VeiculoDto> cadastrarVeiculo(@PathVariable long usuarioId,
            @Valid @RequestBody VeiculoCadastroDto dto) {
        autorizacao.verificarGerenciaUsuario(usuarioId);
        Veiculo criado = servico.criar(usuarioId, conversor.paraEntidade(dto));
        VeiculoDto resposta = conversor.paraResposta(criado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/veiculo/" + criado.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/veiculo/{id}")
    public ResponseEntity<VeiculoDto> obterVeiculo(@PathVariable long id) {
        autorizacao.verificarLeituraVeiculo(id);
        VeiculoDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PutMapping("/veiculo/{id}")
    public ResponseEntity<VeiculoDto> atualizarVeiculo(@PathVariable long id,
            @Valid @RequestBody VeiculoAtualizacaoDto dto) {
        autorizacao.verificarGerenciaVeiculo(id);
        Veiculo atualizado = servico.atualizar(id, conversor.paraEntidade(dto));
        VeiculoDto resposta = conversor.paraResposta(atualizado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @DeleteMapping("/veiculo/{id}")
    public ResponseEntity<Void> excluirVeiculo(@PathVariable long id) {
        autorizacao.verificarGerenciaVeiculo(id);
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
