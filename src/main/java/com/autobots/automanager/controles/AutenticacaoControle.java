package com.autobots.automanager.controles;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.autobots.automanager.dtos.LoginDto;
import com.autobots.automanager.dtos.TokenDto;
import com.autobots.automanager.modelo.AdicionadorLinkToken;
import com.autobots.automanager.servicos.AutenticacaoServico;

@RestController
public class AutenticacaoControle {

    @Autowired
    private AutenticacaoServico servico;
    @Autowired
    private AdicionadorLinkToken adicionadorLink;

    @PostMapping("/login")
    public ResponseEntity<TokenDto> autenticar(@Valid @RequestBody LoginDto dto) {
        TokenDto resposta = servico.autenticar(dto.getNomeUsuario(), dto.getSenha());
        adicionadorLink.adicionarLink(resposta);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, resposta.getTipo() + " " + resposta.getToken())
                .body(resposta);
    }
}
