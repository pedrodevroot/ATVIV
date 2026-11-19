package com.autobots.automanager.servicos;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobots.automanager.dtos.CredencialAtualizacaoDto;
import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.modelo.CredencialAtualizador;
import com.autobots.automanager.modelo.StringVerificadorNulo;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;

@Service
public class CredencialServico {

    @Autowired
    private CredencialRepositorio repositorio;
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private CredencialAtualizador atualizador;
    @Autowired
    private PasswordEncoder codificador;

    private StringVerificadorNulo verificador = new StringVerificadorNulo();

    public List<Credencial> listarPorUsuario(long usuarioId) {
        return obterUsuario(usuarioId).getCredenciais();
    }

    @Transactional
    public Credencial criar(long usuarioId, Credencial credencial) {
        Usuario usuario = obterUsuario(usuarioId);
        credencial.setId(null);
        credencial.setCriacao(Calendar.getInstance().getTime());
        credencial.setInativo(false);
        if (credencial instanceof CredencialUsuarioSenha usuarioSenha) {
            usuarioSenha.setSenha(codificador.encode(usuarioSenha.getSenha()));
        }
        Credencial criada = repositorio.save(credencial);
        usuario.getCredenciais().add(criada);
        usuarioRepositorio.save(usuario);
        return criada;
    }

    public Credencial obterPorId(long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Credencial", id));
    }

    @Transactional
    public Credencial atualizar(long id, CredencialAtualizacaoDto atualizacao) {
        Credencial credencial = obterPorId(id);
        atualizador.atualizar(credencial, atualizacao);
        if (credencial instanceof CredencialUsuarioSenha usuarioSenha
                && !verificador.verificar(atualizacao.getSenha())) {
            usuarioSenha.setSenha(codificador.encode(atualizacao.getSenha()));
        }
        return repositorio.save(credencial);
    }

    @Transactional
    public void excluir(long id) {
        repositorio.delete(obterPorId(id));
    }

    private Usuario obterUsuario(long usuarioId) {
        return usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
    }
}
