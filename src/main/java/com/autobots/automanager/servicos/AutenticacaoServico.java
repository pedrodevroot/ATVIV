package com.autobots.automanager.servicos;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.dtos.TokenDto;
import com.autobots.automanager.jwt.ProvedorJwt;
import com.autobots.automanager.repositorios.CredencialRepositorio;

@Service
public class AutenticacaoServico {

    public static final String TIPO = "Bearer";

    @Autowired
    private AuthenticationManager gerenciador;
    @Autowired
    private ProvedorJwt provedorJwt;
    @Autowired
    private CredencialRepositorio credencialRepositorio;

    @Transactional
    public TokenDto autenticar(String nomeUsuario, String senha) {
        Authentication autenticacao = gerenciador.authenticate(
                new UsernamePasswordAuthenticationToken(nomeUsuario, senha));
        UserDetailsImpl usuario = (UserDetailsImpl) autenticacao.getPrincipal();
        credencialRepositorio.findById(usuario.getCredencialId()).ifPresent(credencial -> {
            credencial.setUltimoAcesso(Calendar.getInstance().getTime());
            credencialRepositorio.save(credencial);
        });
        String jwt = provedorJwt.proverJwt(usuario.getUsername());
        TokenDto token = new TokenDto();
        token.setTipo(TIPO);
        token.setToken(jwt);
        token.setExpiracao(provedorJwt.obterExpiracao(jwt));
        token.setUsuarioId(usuario.getUsuarioId());
        return token;
    }
}
