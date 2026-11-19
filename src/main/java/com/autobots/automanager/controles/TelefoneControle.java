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
import com.autobots.automanager.conversores.TelefoneConversor;
import com.autobots.automanager.dtos.TelefoneAtualizacaoDto;
import com.autobots.automanager.dtos.TelefoneCadastroDto;
import com.autobots.automanager.dtos.TelefoneDto;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.modelo.AdicionadorLinkTelefone;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.TelefoneServico;

@RestController
public class TelefoneControle {

    @Autowired
    private TelefoneServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private TelefoneConversor conversor;
    @Autowired
    private AdicionadorLinkTelefone adicionadorLink;

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/usuario/{usuarioId}/telefones")
    public ResponseEntity<List<TelefoneDto>> obterTelefones(@PathVariable long usuarioId) {
        autorizacao.verificarLeituraUsuario(usuarioId);
        List<TelefoneDto> resposta = conversor.paraResposta(servico.listarPorUsuario(usuarioId));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PostMapping("/usuario/{usuarioId}/telefones")
    public ResponseEntity<TelefoneDto> cadastrarTelefone(@PathVariable long usuarioId,
            @Valid @RequestBody TelefoneCadastroDto dto) {
        autorizacao.verificarGerenciaUsuario(usuarioId);
        Telefone criado = servico.criar(usuarioId, conversor.paraEntidade(dto));
        TelefoneDto resposta = conversor.paraResposta(criado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/telefone/" + criado.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @GetMapping("/empresa/{empresaId}/telefones")
    public ResponseEntity<List<TelefoneDto>> obterTelefonesDaEmpresa(@PathVariable long empresaId) {
        List<TelefoneDto> resposta = conversor.paraResposta(servico.listarPorEmpresa(empresaId));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @PostMapping("/empresa/{empresaId}/telefones")
    public ResponseEntity<TelefoneDto> cadastrarTelefoneDaEmpresa(@PathVariable long empresaId,
            @Valid @RequestBody TelefoneCadastroDto dto) {
        Telefone criado = servico.criarNaEmpresa(empresaId, conversor.paraEntidade(dto));
        TelefoneDto resposta = conversor.paraResposta(criado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/telefone/" + criado.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/telefone/{id}")
    public ResponseEntity<TelefoneDto> obterTelefone(@PathVariable long id) {
        autorizacao.verificarLeituraTelefone(id);
        TelefoneDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PutMapping("/telefone/{id}")
    public ResponseEntity<TelefoneDto> atualizarTelefone(@PathVariable long id,
            @Valid @RequestBody TelefoneAtualizacaoDto dto) {
        autorizacao.verificarGerenciaTelefone(id);
        Telefone atualizado = servico.atualizar(id, conversor.paraEntidade(dto));
        TelefoneDto resposta = conversor.paraResposta(atualizado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @DeleteMapping("/telefone/{id}")
    public ResponseEntity<Void> excluirTelefone(@PathVariable long id) {
        autorizacao.verificarGerenciaTelefone(id);
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
