package com.autobots.automanager.filtros;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.autobots.automanager.jwt.ProvedorJwt;

class AutenticadorJwt {
    private final String jwt;
    private final ProvedorJwt provedorJwt;
    private final UserDetailsService servico;

    AutenticadorJwt(String jwt, ProvedorJwt provedorJwt, UserDetailsService servico) {
        this.jwt = jwt;
        this.provedorJwt = provedorJwt;
        this.servico = servico;
    }

    UsernamePasswordAuthenticationToken obterAutenticacao() {
        if (!provedorJwt.validarJwt(jwt)) {
            return null;
        }
        String nomeUsuario = provedorJwt.obterNomeUsuario(jwt);
        if (nomeUsuario == null) {
            return null;
        }
        UserDetails usuario;
        try {
            usuario = servico.loadUserByUsername(nomeUsuario);
        } catch (UsernameNotFoundException excecao) {
            return null;
        }
        if (usuario == null || !usuario.isEnabled()) {
            return null;
        }
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }
}
