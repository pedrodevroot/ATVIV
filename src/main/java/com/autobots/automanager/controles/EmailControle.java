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
import com.autobots.automanager.conversores.EmailConversor;
import com.autobots.automanager.dtos.EmailAtualizacaoDto;
import com.autobots.automanager.dtos.EmailCadastroDto;
import com.autobots.automanager.dtos.EmailDto;
import com.autobots.automanager.entidades.Email;
import com.autobots.automanager.modelo.AdicionadorLinkEmail;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.EmailServico;

@RestController
public class EmailControle {

    @Autowired
    private EmailServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private EmailConversor conversor;
    @Autowired
    private AdicionadorLinkEmail adicionadorLink;

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/usuario/{usuarioId}/emails")
    public ResponseEntity<List<EmailDto>> obterEmails(@PathVariable long usuarioId) {
        autorizacao.verificarLeituraUsuario(usuarioId);
        List<EmailDto> resposta = conversor.paraResposta(servico.listarPorUsuario(usuarioId));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PostMapping("/usuario/{usuarioId}/emails")
    public ResponseEntity<EmailDto> cadastrarEmail(@PathVariable long usuarioId,
            @Valid @RequestBody EmailCadastroDto dto) {
        autorizacao.verificarGerenciaUsuario(usuarioId);
        Email criado = servico.criar(usuarioId, conversor.paraEntidade(dto));
        EmailDto resposta = conversor.paraResposta(criado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/email/" + criado.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/email/{id}")
    public ResponseEntity<EmailDto> obterEmail(@PathVariable long id) {
        autorizacao.verificarLeituraEmail(id);
        EmailDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PutMapping("/email/{id}")
    public ResponseEntity<EmailDto> atualizarEmail(@PathVariable long id,
            @Valid @RequestBody EmailAtualizacaoDto dto) {
        autorizacao.verificarGerenciaEmail(id);
        Email atualizado = servico.atualizar(id, conversor.paraEntidade(dto));
        EmailDto resposta = conversor.paraResposta(atualizado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @DeleteMapping("/email/{id}")
    public ResponseEntity<Void> excluirEmail(@PathVariable long id) {
        autorizacao.verificarGerenciaEmail(id);
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
