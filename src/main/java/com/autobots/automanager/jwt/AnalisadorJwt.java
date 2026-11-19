package com.autobots.automanager.jwt;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

class AnalisadorJwt {
    private final SecretKey chave;
    private final String jwt;

    AnalisadorJwt(SecretKey chave, String jwt) {
        this.chave = chave;
        this.jwt = jwt;
    }

    Claims obterReivindicacoes() {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        try {
            return Jwts.parserBuilder().setSigningKey(chave).build().parseClaimsJws(jwt).getBody();
        } catch (JwtException | IllegalArgumentException excecao) {
            return null;
        }
    }

    String obterNomeUsuario(Claims reivindicacoes) {
        return reivindicacoes == null ? null : reivindicacoes.getSubject();
    }
}
