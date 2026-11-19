package com.autobots.automanager.jwt;

import java.util.Date;

import io.jsonwebtoken.Claims;

class ValidadorJwt {

    boolean validar(Claims reivindicacoes) {
        if (reivindicacoes == null) {
            return false;
        }
        String nomeUsuario = reivindicacoes.getSubject();
        Date expiracao = reivindicacoes.getExpiration();
        return nomeUsuario != null && !nomeUsuario.isBlank()
                && expiracao != null && expiracao.after(new Date());
    }
}
