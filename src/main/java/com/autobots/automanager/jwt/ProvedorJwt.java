package com.autobots.automanager.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

@Component
public class ProvedorJwt {
    private final SecretKey chave;
    private final long duracao;

    public ProvedorJwt(@Value("${jwt.secret}") String segredo, @Value("${jwt.expiration}") long duracao) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            throw new IllegalArgumentException("A propriedade jwt.secret deve ter ao menos 64 bytes");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.duracao = duracao;
    }

    public String proverJwt(String nomeUsuario) {
        return new GeradorJwt(chave, duracao).gerarJwt(nomeUsuario);
    }

    public boolean validarJwt(String jwt) {
        Claims reivindicacoes = new AnalisadorJwt(chave, jwt).obterReivindicacoes();
        return new ValidadorJwt().validar(reivindicacoes);
    }

    public String obterNomeUsuario(String jwt) {
        AnalisadorJwt analisador = new AnalisadorJwt(chave, jwt);
        return analisador.obterNomeUsuario(analisador.obterReivindicacoes());
    }

    public Date obterExpiracao(String jwt) {
        Claims reivindicacoes = new AnalisadorJwt(chave, jwt).obterReivindicacoes();
        return reivindicacoes == null ? null : reivindicacoes.getExpiration();
    }
}
