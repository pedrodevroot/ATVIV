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
import com.autobots.automanager.conversores.UsuarioConversor;
import com.autobots.automanager.dtos.UsuarioAtualizacaoDto;
import com.autobots.automanager.dtos.UsuarioCadastroDto;
import com.autobots.automanager.dtos.UsuarioRespostaDto;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.modelo.AdicionadorLinkUsuario;
import com.autobots.automanager.servicos.AutorizacaoServico;
import com.autobots.automanager.servicos.UsuarioServico;

@RestController
@RequestMapping("/usuario")
public class UsuarioControle {

    @Autowired
    private UsuarioServico servico;
    @Autowired
    private AutorizacaoServico autorizacao;
    @Autowired
    private UsuarioConversor conversor;
    @Autowired
    private AdicionadorLinkUsuario adicionadorLink;

    @PreAuthorize(Autorizacoes.PERFIS_DE_ACESSO)
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioRespostaDto> obterUsuario(@PathVariable long id) {
        autorizacao.verificarLeituraUsuario(id);
        UsuarioRespostaDto resposta = conversor.paraResposta(servico.obterPorId(id));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioRespostaDto>> obterUsuarios() {
        List<UsuarioRespostaDto> resposta = conversor.paraResposta(autorizacao.filtrarUsuarios(servico.listar()));
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioRespostaDto> cadastrarUsuario(
            @Valid @RequestBody UsuarioCadastroDto dto) {
        autorizacao.verificarPerfisAtribuiveis(dto.getPerfis());
        Usuario cadastrado = servico.cadastrar(conversor.paraEntidade(dto));
        UsuarioRespostaDto resposta = conversor.paraResposta(cadastrado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.created(URI.create("/usuario/" + cadastrado.getId()))
                .body(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @PutMapping("/atualizar")
    public ResponseEntity<UsuarioRespostaDto> atualizarUsuario(
            @Valid @RequestBody UsuarioAtualizacaoDto dto) {
        autorizacao.verificarGerenciaUsuario(dto.getId());
        if (dto.getPerfis() != null) {
            autorizacao.verificarPerfisAtribuiveis(dto.getPerfis());
        }
        Usuario atualizado = servico.atualizar(conversor.paraEntidade(dto));
        UsuarioRespostaDto resposta = conversor.paraResposta(atualizado);
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize(Autorizacoes.FUNCIONARIOS)
    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<Void> excluirUsuario(@PathVariable long id) {
        autorizacao.verificarGerenciaUsuario(id);
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
