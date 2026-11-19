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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobots.automanager.configuracao.Autorizacoes;
import com.autobots.automanager.conversores.EmpresaConversor;
import com.autobots.automanager.conversores.UsuarioConversor;
import com.autobots.automanager.dtos.EmpresaAtualizacaoDto;
import com.autobots.automanager.dtos.EmpresaCadastroDto;
import com.autobots.automanager.dtos.EmpresaRespostaDto;
import com.autobots.automanager.dtos.UsuarioRespostaDto;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.modelo.AdicionadorLinkEmpresa;
import com.autobots.automanager.modelo.AdicionadorLinkUsuario;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.EmpresaServico;

@RestController
@RequestMapping("/empresa")
public class EmpresaControle {

    @Autowired
    private EmpresaServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private EmpresaConversor conversor;
    @Autowired
    private UsuarioConversor usuarioConversor;
    @Autowired
    private AdicionadorLinkEmpresa adicionadorLink;
    @Autowired
    private AdicionadorLinkUsuario adicionadorLinkUsuario;

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaRespostaDto> obterEmpresa(@PathVariable long id) {
        EmpresaRespostaDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @GetMapping("/empresas")
    public ResponseEntity<List<EmpresaRespostaDto>> obterEmpresas() {
        List<EmpresaRespostaDto> resposta = conversor.paraResposta(servico.listar());
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @PostMapping("/cadastro")
    public ResponseEntity<EmpresaRespostaDto> cadastrarEmpresa(
            @Valid @RequestBody EmpresaCadastroDto dto) {
        Empresa cadastrada = servico.cadastrar(conversor.paraEntidade(dto));
        EmpresaRespostaDto resposta = conversor.paraResposta(cadastrada);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/empresa/" + cadastrada.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @PutMapping("/atualizar")
    public ResponseEntity<EmpresaRespostaDto> atualizarEmpresa(
            @Valid @RequestBody EmpresaAtualizacaoDto dto) {
        Empresa atualizada = servico.atualizar(conversor.paraEntidade(dto));
        EmpresaRespostaDto resposta = conversor.paraResposta(atualizada);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.ADMINISTRADOR)
    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<Void> excluirEmpresa(@PathVariable long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @GetMapping("/{empresaId}/usuarios")
    public ResponseEntity<List<UsuarioRespostaDto>> obterUsuarios(@PathVariable long empresaId) {
        List<UsuarioRespostaDto> resposta = usuarioConversor
                .paraResposta(autorizacao.filtrarUsuarios(servico.listarUsuarios(empresaId)));
        adicionadorLinkUsuario.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PutMapping("/{empresaId}/usuarios/{usuarioId}")
    public ResponseEntity<Void> vincularUsuario(@PathVariable long empresaId,
            @PathVariable long usuarioId) {
        autorizacao.verificarGerenciaUsuario(usuarioId);
        servico.vincularUsuario(empresaId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @DeleteMapping("/{empresaId}/usuarios/{usuarioId}")
    public ResponseEntity<Void> desvincularUsuario(@PathVariable long empresaId,
            @PathVariable long usuarioId) {
        autorizacao.verificarGerenciaUsuario(usuarioId);
        servico.desvincularUsuario(empresaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
