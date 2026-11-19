package com.autobots.automanager.adaptadores;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private CredencialRepositorio credencialRepositorio;
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nomeUsuario) {
        CredencialUsuarioSenha credencial = credencialRepositorio.buscarPorNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Nome de usuario nao encontrado: " + nomeUsuario));
        Usuario usuario = usuarioRepositorio.findByCredenciaisId(credencial.getId())
                .orElseThrow(() -> new UsernameNotFoundException("A credencial nao pertence a nenhum usuario"));
        boolean temPerfilDeAcesso = usuario.getPerfis().stream().anyMatch(perfil -> perfil != PerfilUsuario.FORNECEDOR);
        if (!temPerfilDeAcesso) {
            throw new UsernameNotFoundException("Usuario sem perfil de acesso");
        }
        return new UserDetailsImpl(usuario.getId(), credencial.getId(), credencial.getNomeUsuario(),
                credencial.getSenha(), Set.copyOf(usuario.getPerfis()), !credencial.isInativo());
    }
}
