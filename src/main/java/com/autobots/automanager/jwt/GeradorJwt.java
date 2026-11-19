package com.autobots.automanager.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

class GeradorJwt {
    private final SecretKey chave;
    private final long duracao;

    GeradorJwt(SecretKey chave, long duracao) {
        this.chave = chave;
        this.duracao = duracao;
    }

    String gerarJwt(String nomeUsuario) {
        Date agora = new Date();
        return Jwts.builder()
                .setSubject(nomeUsuario)
                .setIssuedAt(agora)
                .setExpiration(new Date(agora.getTime() + duracao))
                .signWith(chave, SignatureAlgorithm.HS512)
                .compact();
    }
}
