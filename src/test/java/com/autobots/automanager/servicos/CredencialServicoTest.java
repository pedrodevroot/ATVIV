package com.autobots.automanager.servicos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.autobots.automanager.dtos.CredencialAtualizacaoDto;
import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.entidades.CredencialCodigoBarra;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.excecoes.RecursoNaoEncontradoException;
import com.autobots.automanager.modelo.CredencialAtualizador;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class CredencialServicoTest {

    @Mock
    private CredencialRepositorio repositorio;
    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private CredencialAtualizador atualizador;
    @Mock
    private PasswordEncoder codificador;

    @InjectMocks
    private CredencialServico servico;

    private CredencialUsuarioSenha credencial(Long id, String nomeUsuario) {
        CredencialUsuarioSenha credencial = new CredencialUsuarioSenha();
        credencial.setId(id);
        credencial.setNomeUsuario(nomeUsuario);
        credencial.setSenha("segredo1");
        return credencial;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Maria");
        return usuario;
    }

    @Test
    @DisplayName("listarPorUsuario: devolve as credenciais do usuario")
    void listarPorUsuario() {
        Usuario usuario = usuario();
        usuario.getCredenciais().add(credencial(1L, "maria"));
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario));

        assertEquals(1, servico.listarPorUsuario(1L).size());
    }

    @Test
    @DisplayName("criar: a credencial nasce ativa, sem id e com a data de criacao do servidor")
    void criarDefineEstadoInicial() {
        Usuario usuario = usuario();
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario));
        when(repositorio.save(any(Credencial.class))).thenAnswer(chamada -> {
            Credencial argumento = chamada.getArgument(0);
            assertNull(argumento.getId());
            argumento.setId(99L);
            return argumento;
        });
        CredencialUsuarioSenha entrada = credencial(7L, "maria");
        entrada.setInativo(true);

        Credencial criada = servico.criar(1L, entrada);

        assertEquals(99L, criada.getId());
        assertNotNull(criada.getCriacao());
        assertFalse(criada.isInativo());
        assertNull(criada.getUltimoAcesso());
        assertSame(criada, usuario.getCredenciais().get(0));
        verify(usuarioRepositorio).save(usuario);
    }

    @Test
    @DisplayName("criar: lanca RecursoNaoEncontrado quando o usuario nao existe")
    void criarUsuarioInexistente() {
        when(usuarioRepositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> servico.criar(999L, credencial(null, "maria")));

        verify(repositorio, never()).save(any(Credencial.class));
    }

    @Test
    @DisplayName("obterPorId: lanca RecursoNaoEncontrado quando o id nao existe")
    void obterPorIdInexistente() {
        when(repositorio.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException excecao = assertThrows(
                RecursoNaoEncontradoException.class, () -> servico.obterPorId(999L));

        assertEquals("Credencial de id 999 nao foi encontrado", excecao.getMessage());
    }

    @Test
    @DisplayName("atualizar: delega ao CredencialAtualizador e salva")
    void atualizarDelegaAoAtualizador() {
        Credencial existente = credencial(2L, "maria");
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setInativo(true);
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        servico.atualizar(2L, atualizacao);

        verify(atualizador).atualizar(existente, atualizacao);
        verify(repositorio).save(existente);
    }

    @Test
    @DisplayName("criar: codifica a senha da credencial de usuario e senha")
    void criarCodificaSenha() {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario()));
        when(repositorio.save(any(Credencial.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(codificador.encode("segredo1")).thenReturn("codificada");

        CredencialUsuarioSenha criada = (CredencialUsuarioSenha) servico.criar(1L, credencial(null, "maria"));

        assertEquals("codificada", criada.getSenha());
    }

    @Test
    @DisplayName("criar: nao usa o codificador para credencial de codigo de barras")
    void criarCodigoDeBarrasSemCodificador() {
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario()));
        when(repositorio.save(any(Credencial.class))).thenAnswer(chamada -> chamada.getArgument(0));
        CredencialCodigoBarra entrada = new CredencialCodigoBarra();
        entrada.setCodigo(123456L);

        servico.criar(1L, entrada);

        verifyNoInteractions(codificador);
    }

    @Test
    @DisplayName("atualizar: com senha nova grava o valor codificado")
    void atualizarCodificaSenhaNova() {
        CredencialUsuarioSenha existente = credencial(2L, "maria");
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setSenha("novasenha1");
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);
        when(codificador.encode("novasenha1")).thenReturn("codificada");

        servico.atualizar(2L, atualizacao);

        assertEquals("codificada", existente.getSenha());
    }

    @Test
    @DisplayName("atualizar: com o atualizador real, so a senha codificada chega a credencial")
    void atualizarComAtualizadorRealGravaSoSenhaCodificada() {
        ReflectionTestUtils.setField(servico, "atualizador", new CredencialAtualizador());
        CredencialUsuarioSenha existente = credencial(2L, "maria");
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setSenha("novasenha1");
        atualizacao.setNomeUsuario("mariaSilva");
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);
        when(codificador.encode("novasenha1")).thenReturn("codificada");

        servico.atualizar(2L, atualizacao);

        assertEquals("codificada", existente.getSenha());
        assertEquals("mariaSilva", existente.getNomeUsuario());
        verify(codificador).encode("novasenha1");
    }

    @Test
    @DisplayName("atualizar: com o atualizador real e sem senha, a senha atual e preservada")
    void atualizarComAtualizadorRealSemSenhaPreservaHash() {
        ReflectionTestUtils.setField(servico, "atualizador", new CredencialAtualizador());
        CredencialUsuarioSenha existente = credencial(2L, "maria");
        existente.setSenha("hashAtual");
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setInativo(true);
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        servico.atualizar(2L, atualizacao);

        assertEquals("hashAtual", existente.getSenha());
        assertEquals(true, existente.isInativo());
        verifyNoInteractions(codificador);
    }

    @Test
    @DisplayName("atualizar: sem senha nao usa o codificador")
    void atualizarSemSenhaNaoCodifica() {
        CredencialUsuarioSenha existente = credencial(2L, "maria");
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setInativo(true);
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        servico.atualizar(2L, atualizacao);

        verifyNoInteractions(codificador);
    }

    @Test
    @DisplayName("atualizar: credencial de codigo de barras nao usa o codificador")
    void atualizarCodigoDeBarrasSemCodificador() {
        CredencialCodigoBarra existente = new CredencialCodigoBarra();
        existente.setId(3L);
        existente.setCodigo(111L);
        CredencialAtualizacaoDto atualizacao = new CredencialAtualizacaoDto();
        atualizacao.setSenha("novasenha1");
        when(repositorio.findById(3L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        servico.atualizar(3L, atualizacao);

        verifyNoInteractions(codificador);
    }

    @Test
    @DisplayName("excluir: remove a credencial encontrada")
    void excluirExistente() {
        Credencial existente = credencial(2L, "maria");
        when(repositorio.findById(2L)).thenReturn(Optional.of(existente));

        servico.excluir(2L);

        verify(repositorio).delete(existente);
    }

    @Test
    @DisplayName("excluir: lanca RecursoNaoEncontrado quando o id nao existe")
    void excluirIdInexistente() {
        when(repositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.excluir(999L));

        verify(repositorio, never()).delete(any(Credencial.class));
    }
}
