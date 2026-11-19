package com.autobots.automanager.repositorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.autobots.automanager.entidades.CredencialCodigoBarra;
import com.autobots.automanager.entidades.CredencialUsuarioSenha;
import com.autobots.automanager.entidades.Documento;
import com.autobots.automanager.entidades.Email;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.enumeracoes.TipoDocumento;

@DataJpaTest
class ConsultasAcessoJpaTest {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private EmpresaRepositorio empresaRepositorio;
    @Autowired
    private CredencialRepositorio credencialRepositorio;
    @Autowired
    private TestEntityManager gerenciador;

    private Usuario usuarioCompleto(String nome, String nomeUsuario, String numeroDocumento) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.getPerfis().add(PerfilUsuario.CLIENTE);

        Telefone telefone = new Telefone();
        telefone.setDdd("13");
        telefone.setNumero("98123" + numeroDocumento);
        usuario.getTelefones().add(telefone);

        Documento documento = new Documento();
        documento.setTipo(TipoDocumento.RG);
        documento.setNumero(numeroDocumento);
        documento.setDataEmissao(new Date());
        usuario.getDocumentos().add(documento);

        Email email = new Email();
        email.setEndereco(nomeUsuario + "@email.com");
        usuario.getEmails().add(email);

        CredencialUsuarioSenha credencial = new CredencialUsuarioSenha();
        credencial.setNomeUsuario(nomeUsuario);
        credencial.setSenha("segredo1");
        credencial.setCriacao(new Date());
        usuario.getCredenciais().add(credencial);

        return usuario;
    }

    private Empresa empresaComTelefone(String numero) {
        Empresa empresa = new Empresa();
        empresa.setRazaoSocial("Empresa Teste");
        empresa.setCadastro(new Date());
        Telefone telefone = new Telefone();
        telefone.setDdd("13");
        telefone.setNumero(numero);
        empresa.getTelefones().add(telefone);
        return empresa;
    }

    @Test
    @DisplayName("buscarPorNomeUsuario encontra a credencial salva")
    void buscarPorNomeUsuarioEncontra() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));

        CredencialUsuarioSenha encontrada = credencialRepositorio.buscarPorNomeUsuario("ana-login").orElseThrow();

        assertEquals("ana-login", encontrada.getNomeUsuario());
        assertEquals(usuario.getCredenciais().get(0).getId(), encontrada.getId());
    }

    @Test
    @DisplayName("buscarPorNomeUsuario devolve vazio para nome inexistente")
    void buscarPorNomeUsuarioVazio() {
        usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));

        assertTrue(credencialRepositorio.buscarPorNomeUsuario("inexistente").isEmpty());
    }

    @Test
    @DisplayName("buscarPorNomeUsuario ignora credencial de codigo de barras")
    void buscarPorNomeUsuarioIgnoraCodigoBarra() {
        Usuario usuario = new Usuario();
        usuario.setNome("Beto");
        usuario.getPerfis().add(PerfilUsuario.CLIENTE);
        CredencialCodigoBarra credencial = new CredencialCodigoBarra();
        credencial.setCodigo(123456789L);
        credencial.setCriacao(new Date());
        usuario.getCredenciais().add(credencial);
        usuarioRepositorio.saveAndFlush(usuario);

        assertTrue(credencialRepositorio.buscarPorNomeUsuario("123456789").isEmpty());
    }

    @Test
    @DisplayName("findByCredenciaisId devolve o dono da credencial")
    void buscarUsuarioPorCredencial() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));
        Long credencialId = usuario.getCredenciais().get(0).getId();

        assertEquals(usuario.getId(), usuarioRepositorio.findByCredenciaisId(credencialId).orElseThrow().getId());
        assertTrue(usuarioRepositorio.findByCredenciaisId(999999L).isEmpty());
    }

    @Test
    @DisplayName("findByTelefonesId devolve o usuario dono e vazio para telefone de empresa")
    void buscarUsuarioPorTelefone() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));
        Empresa empresa = empresaRepositorio.saveAndFlush(empresaComTelefone("30001111"));
        gerenciador.clear();

        Long telefoneUsuarioId = usuario.getTelefones().get(0).getId();
        Long telefoneEmpresaId = empresa.getTelefones().get(0).getId();

        assertEquals(usuario.getId(), usuarioRepositorio.findByTelefonesId(telefoneUsuarioId).orElseThrow().getId());
        assertTrue(usuarioRepositorio.findByTelefonesId(telefoneEmpresaId).isEmpty());
    }

    @Test
    @DisplayName("findByDocumentosId devolve o dono do documento")
    void buscarUsuarioPorDocumento() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));
        Long documentoId = usuario.getDocumentos().get(0).getId();

        assertEquals(usuario.getId(), usuarioRepositorio.findByDocumentosId(documentoId).orElseThrow().getId());
    }

    @Test
    @DisplayName("findByEmailsId devolve o dono do e-mail")
    void buscarUsuarioPorEmail() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));
        Long emailId = usuario.getEmails().get(0).getId();

        assertEquals(usuario.getId(), usuarioRepositorio.findByEmailsId(emailId).orElseThrow().getId());
    }

    @Test
    @DisplayName("EmpresaRepositorio findByTelefonesId devolve a empresa dona e vazio para telefone de usuario")
    void buscarEmpresaPorTelefone() {
        Usuario usuario = usuarioRepositorio.saveAndFlush(usuarioCompleto("Ana", "ana-login", "1001"));
        Empresa empresa = empresaRepositorio.saveAndFlush(empresaComTelefone("30001111"));
        gerenciador.clear();

        Long telefoneEmpresaId = empresa.getTelefones().get(0).getId();
        Long telefoneUsuarioId = usuario.getTelefones().get(0).getId();

        assertEquals(empresa.getId(), empresaRepositorio.findByTelefonesId(telefoneEmpresaId).orElseThrow().getId());
        assertTrue(empresaRepositorio.findByTelefonesId(telefoneUsuarioId).isEmpty());
    }
}
