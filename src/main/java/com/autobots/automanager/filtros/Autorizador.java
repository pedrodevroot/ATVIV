package com.autobots.automanager.filtros;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.autobots.automanager.jwt.ProvedorJwt;

public class Autorizador extends OncePerRequestFilter {
    private final ProvedorJwt provedorJwt;
    private final UserDetailsService servico;

    public Autorizador(ProvedorJwt provedorJwt, UserDetailsService servico) {
        this.provedorJwt = provedorJwt;
        this.servico = servico;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (new ValidadorCabecalho(cabecalho).validar()) {
            String jwt = new AnalisadorCabecalho(cabecalho).obterJwt();
            UsernamePasswordAuthenticationToken autenticacao =
                    new AutenticadorJwt(jwt, provedorJwt, servico).obterAutenticacao();
            if (autenticacao != null) {
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
