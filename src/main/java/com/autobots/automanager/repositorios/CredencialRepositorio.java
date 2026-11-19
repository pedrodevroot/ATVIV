package com.autobots.automanager.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;

public interface CredencialRepositorio extends JpaRepository<Credencial, Long> {
    @Query("select c from CredencialUsuarioSenha c where c.nomeUsuario = :nomeUsuario")
    Optional<CredencialUsuarioSenha> buscarPorNomeUsuario(@Param("nomeUsuario") String nomeUsuario);
}
