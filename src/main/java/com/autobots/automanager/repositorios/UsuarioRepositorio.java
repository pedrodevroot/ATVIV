package com.autobots.automanager.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobots.automanager.entidades.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEnderecoId(Long enderecoId);

    Optional<Usuario> findByMercadoriasId(Long mercadoriaId);

    Optional<Usuario> findByCredenciaisId(Long credencialId);

    Optional<Usuario> findByTelefonesId(Long telefoneId);

    Optional<Usuario> findByDocumentosId(Long documentoId);

    Optional<Usuario> findByEmailsId(Long emailId);
}
