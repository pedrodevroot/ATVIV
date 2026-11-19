package com.autobots.automanager.servicos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.entidades.Veiculo;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.repositorios.VeiculoRepositorio;
import com.autobots.automanager.repositorios.VendaRepositorio;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServicoTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private EmpresaRepositorio empresaRepositorio;
    @Mock
    private VeiculoRepositorio veiculoRepositorio;
    @Mock
    private VendaRepositorio vendaRepositorio;

    @InjectMocks
    private AutorizacaoServico servico;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void logar(Long id, PerfilUsuario... perfis) {
        UserDetailsImpl usuario = new UserDetailsImpl(id, id, "usuario" + id, "hash", Set.of(perfis), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Usuario usuario(Long id, PerfilUsuario... perfis) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPerfis(new HashSet<>(Set.of(perfis)));
        return usuario;
    }

    private Venda venda(Long id, Usuario cliente, Usuario funcionario) {
        Venda venda = new Venda();
        venda.setId(id);
        venda.setCliente(cliente);
        venda.setFuncionario(funcionario);
        return venda;
    }

    private void assertNega(Executable acao) {
        AccessDeniedException excecao = assertThrows(AccessDeniedException.class, acao);
        assertEquals(RespostaSeguranca.ACESSO_NEGADO, excecao.getMessage());
    }

    private void verificarSemConsultas() {
        verifyNoInteractions(usuarioRepositorio, empresaRepositorio, veiculoRepositorio, vendaRepositorio);
    }

    @Test
    @DisplayName("perfisDoUsuarioLogado le so as autoridades ROLE_<PERFIL> e ignora as demais")
    void perfisIgnoramAutoridadesEstranhas() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ana", null,
                List.of(new SimpleGrantedAuthority("ROLE_GERENTE"), new SimpleGrantedAuthority("ROLE_ANONYMOUS"),
                        new SimpleGrantedAuthority("CLIENTE"), new SimpleGrantedAuthority("ROLE_FORNECEDOR"),
                        new SimpleGrantedAuthority("LEITURA"))));

        assertEquals(EnumSet.of(PerfilUsuario.GERENTE, PerfilUsuario.FORNECEDOR), servico.perfisDoUsuarioLogado());
    }

    @Test
    @DisplayName("sem autenticacao: nenhum perfil, id nulo, nao e administrador e nada e gerenciavel")
    void semAutenticacao() {
        assertTrue(servico.perfisDoUsuarioLogado().isEmpty());
        assertNull(servico.idDoUsuarioLogado());
        assertFalse(servico.ehAdministrador());
        assertTrue(servico.perfisGerenciaveis().isEmpty());
    }

    @Test
    @DisplayName("idDoUsuarioLogado e nulo quando o principal nao e UserDetailsImpl")
    void idComPrincipalQueNaoEUserDetailsImpl() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ana", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));

        assertNull(servico.idDoUsuarioLogado());
        assertTrue(servico.ehAdministrador());
    }

    @Test
    @DisplayName("idDoUsuarioLogado devolve o usuarioId do UserDetailsImpl")
    void idDoUserDetailsImpl() {
        logar(7L, PerfilUsuario.CLIENTE);

        assertEquals(7L, servico.idDoUsuarioLogado());
    }

    @Test
    @DisplayName("perfisGerenciaveis: administrador gerencia todos os perfis")
    void gerenciaveisAdministrador() {
        logar(3L, PerfilUsuario.ADMINISTRADOR);

        assertEquals(EnumSet.allOf(PerfilUsuario.class), servico.perfisGerenciaveis());
    }

    @Test
    @DisplayName("perfisGerenciaveis: gerente gerencia gerente, vendedor e cliente")
    void gerenciaveisGerente() {
        logar(4L, PerfilUsuario.GERENTE);

        assertEquals(EnumSet.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE),
                servico.perfisGerenciaveis());
    }

    @Test
    @DisplayName("perfisGerenciaveis: vendedor gerencia so cliente")
    void gerenciaveisVendedor() {
        logar(1L, PerfilUsuario.VENDEDOR);

        assertEquals(EnumSet.of(PerfilUsuario.CLIENTE), servico.perfisGerenciaveis());
    }

    @Test
    @DisplayName("perfisGerenciaveis: cliente e fornecedor nao gerenciam ninguem")
    void gerenciaveisClienteEFornecedor() {
        logar(2L, PerfilUsuario.CLIENTE);
        assertTrue(servico.perfisGerenciaveis().isEmpty());

        logar(9L, PerfilUsuario.FORNECEDOR);
        assertTrue(servico.perfisGerenciaveis().isEmpty());
    }

    @Test
    @DisplayName("perfisGerenciaveis: gerente e cliente ao mesmo tempo usa a uniao")
    void gerenciaveisGerenteECliente() {
        logar(4L, PerfilUsuario.GERENTE, PerfilUsuario.CLIENTE);

        assertEquals(EnumSet.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE),
                servico.perfisGerenciaveis());
    }

    @Test
    @DisplayName("administrador passa em todas as verificacoes sem consultar nenhum repositorio")
    void administradorNaoConsulta() {
        logar(3L, PerfilUsuario.ADMINISTRADOR);

        assertDoesNotThrow(() -> {
            servico.verificarLeituraUsuario(10L);
            servico.verificarGerenciaUsuario(10L);
            servico.verificarPerfisAtribuiveis(Set.of(PerfilUsuario.ADMINISTRADOR, PerfilUsuario.FORNECEDOR));
            servico.verificarLeituraTelefone(10L);
            servico.verificarGerenciaTelefone(10L);
            servico.verificarLeituraDocumento(10L);
            servico.verificarGerenciaDocumento(10L);
            servico.verificarLeituraEmail(10L);
            servico.verificarGerenciaEmail(10L);
            servico.verificarLeituraCredencial(10L);
            servico.verificarGerenciaCredencial(10L);
            servico.verificarLeituraEndereco(10L);
            servico.verificarGerenciaEndereco(10L);
            servico.verificarLeituraVeiculo(10L);
            servico.verificarGerenciaVeiculo(10L);
            servico.verificarLeituraVenda(10L);
            servico.verificarCriacaoVenda(99L);
        });
        verificarSemConsultas();
    }

    @Test
    @DisplayName("administrador: filtros devolvem a propria lista recebida")
    void administradorFiltrosDevolvemAMesmaLista() {
        logar(3L, PerfilUsuario.ADMINISTRADOR);
        List<Usuario> usuarios = List.of(usuario(1L, PerfilUsuario.FORNECEDOR), usuario(2L, PerfilUsuario.CLIENTE));
        List<Venda> vendas = List.of(
                venda(1L, usuario(2L, PerfilUsuario.CLIENTE), usuario(1L, PerfilUsuario.VENDEDOR)));

        assertSame(usuarios, servico.filtrarUsuarios(usuarios));
        assertSame(vendas, servico.filtrarVendas(vendas));
        assertTrue(servico.podeGerenciar(usuario(5L, PerfilUsuario.ADMINISTRADOR)));
        verificarSemConsultas();
    }

    @Test
    @DisplayName("gerente nao pode atribuir o perfil ADMINISTRADOR")
    void gerenteNaoCriaAdministrador() {
        logar(4L, PerfilUsuario.GERENTE);

        assertNega(() -> servico.verificarPerfisAtribuiveis(Set.of(PerfilUsuario.ADMINISTRADOR)));
        assertNega(() -> servico.verificarPerfisAtribuiveis(Set.of(PerfilUsuario.CLIENTE, PerfilUsuario.FORNECEDOR)));
        assertDoesNotThrow(() -> servico.verificarPerfisAtribuiveis(
                Set.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE)));
    }

    @Test
    @DisplayName("perfis nulos ou vazios so sao atribuiveis pelo administrador")
    void perfisNulosOuVazios() {
        logar(3L, PerfilUsuario.ADMINISTRADOR);

        assertDoesNotThrow(() -> servico.verificarPerfisAtribuiveis(null));
        assertDoesNotThrow(() -> servico.verificarPerfisAtribuiveis(Set.of()));
        verificarSemConsultas();
    }

    @Test
    @DisplayName("gerente, vendedor e cliente nao atribuem perfis nulos ou vazios")
    void perfisNulosOuVaziosNegadosParaNaoAdministrador() {
        for (PerfilUsuario perfil : List.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE)) {
            logar(2L, perfil);

            assertNega(() -> servico.verificarPerfisAtribuiveis(null));
            assertNega(() -> servico.verificarPerfisAtribuiveis(Set.of()));
            assertNega(() -> servico.verificarPerfisAtribuiveis(new HashSet<>()));
        }
    }

    @Test
    @DisplayName("usuario sem perfis (vazio ou nulo) so e gerenciavel pelo administrador")
    void usuarioSemPerfisSoEGerenciavelPeloAdministrador() {
        Usuario semPerfis = usuario(8L);
        Usuario perfisNulos = usuario(9L);
        perfisNulos.setPerfis(null);

        for (PerfilUsuario perfil : List.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE,
                PerfilUsuario.FORNECEDOR)) {
            logar(2L, perfil);

            assertFalse(servico.podeGerenciar(semPerfis));
            assertFalse(servico.podeGerenciar(perfisNulos));
            assertFalse(servico.podeLer(semPerfis));
            assertFalse(servico.podeLer(perfisNulos));
        }
        logar(3L, PerfilUsuario.ADMINISTRADOR);
        assertTrue(servico.podeGerenciar(semPerfis));
        assertTrue(servico.podeGerenciar(perfisNulos));
    }

    @Test
    @DisplayName("usuario sem perfis nao e gerenciado por gerente nem vendedor, mas o proprio le o cadastro")
    void verificacoesComUsuarioSemPerfis() {
        when(usuarioRepositorio.findById(8L)).thenReturn(Optional.of(usuario(8L)));

        logar(4L, PerfilUsuario.GERENTE);
        assertNega(() -> servico.verificarGerenciaUsuario(8L));
        assertNega(() -> servico.verificarLeituraUsuario(8L));

        logar(1L, PerfilUsuario.VENDEDOR);
        assertNega(() -> servico.verificarGerenciaUsuario(8L));

        logar(8L, PerfilUsuario.CLIENTE);
        assertNega(() -> servico.verificarGerenciaUsuario(8L));
        assertDoesNotThrow(() -> servico.verificarLeituraUsuario(8L));
    }

    @Test
    @DisplayName("gerente nao le, nao altera e nao exclui usuario administrador")
    void gerenteNaoGerenciaAdministrador() {
        logar(4L, PerfilUsuario.GERENTE);
        when(usuarioRepositorio.findById(3L)).thenReturn(Optional.of(usuario(3L, PerfilUsuario.ADMINISTRADOR)));

        assertNega(() -> servico.verificarLeituraUsuario(3L));
        assertNega(() -> servico.verificarGerenciaUsuario(3L));
    }

    @Test
    @DisplayName("gerente nao le, nao altera e nao exclui usuario fornecedor")
    void gerenteNaoGerenciaFornecedor() {
        logar(4L, PerfilUsuario.GERENTE);
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuario(2L, PerfilUsuario.FORNECEDOR)));

        assertNega(() -> servico.verificarLeituraUsuario(2L));
        assertNega(() -> servico.verificarGerenciaUsuario(2L));
    }

    @Test
    @DisplayName("gerente gerencia gerente, vendedor e cliente, inclusive ele mesmo")
    void gerenteGerenciaGerenteVendedorCliente() {
        logar(4L, PerfilUsuario.GERENTE);
        when(usuarioRepositorio.findById(4L)).thenReturn(Optional.of(usuario(4L, PerfilUsuario.GERENTE)));
        when(usuarioRepositorio.findById(6L)).thenReturn(Optional.of(usuario(6L, PerfilUsuario.GERENTE)));
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario(1L, PerfilUsuario.VENDEDOR)));
        when(usuarioRepositorio.findById(2L))
                .thenReturn(Optional.of(usuario(2L, PerfilUsuario.CLIENTE, PerfilUsuario.VENDEDOR)));

        assertDoesNotThrow(() -> {
            servico.verificarGerenciaUsuario(4L);
            servico.verificarGerenciaUsuario(6L);
            servico.verificarGerenciaUsuario(1L);
            servico.verificarGerenciaUsuario(2L);
            servico.verificarLeituraUsuario(2L);
        });
    }

    @Test
    @DisplayName("vendedor gerencia usuario cliente")
    void vendedorGerenciaCliente() {
        logar(1L, PerfilUsuario.VENDEDOR);
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuario(2L, PerfilUsuario.CLIENTE)));

        assertDoesNotThrow(() -> servico.verificarGerenciaUsuario(2L));
        assertDoesNotThrow(() -> servico.verificarLeituraUsuario(2L));
        assertDoesNotThrow(() -> servico.verificarPerfisAtribuiveis(Set.of(PerfilUsuario.CLIENTE)));
    }

    @Test
    @DisplayName("vendedor nao gerencia usuario que e cliente e vendedor")
    void vendedorNaoGerenciaClienteVendedor() {
        logar(1L, PerfilUsuario.VENDEDOR);
        when(usuarioRepositorio.findById(6L))
                .thenReturn(Optional.of(usuario(6L, PerfilUsuario.CLIENTE, PerfilUsuario.VENDEDOR)));

        assertNega(() -> servico.verificarGerenciaUsuario(6L));
        assertNega(() -> servico.verificarLeituraUsuario(6L));
    }

    @Test
    @DisplayName("vendedor nao promove cliente a vendedor")
    void vendedorNaoPromoveCliente() {
        logar(1L, PerfilUsuario.VENDEDOR);

        assertNega(() -> servico.verificarPerfisAtribuiveis(Set.of(PerfilUsuario.CLIENTE, PerfilUsuario.VENDEDOR)));
    }

    @Test
    @DisplayName("vendedor le o proprio cadastro, mas nao o gerencia")
    void vendedorLeOProprioCadastro() {
        logar(1L, PerfilUsuario.VENDEDOR);
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario(1L, PerfilUsuario.VENDEDOR)));

        assertDoesNotThrow(() -> servico.verificarLeituraUsuario(1L));
        assertNega(() -> servico.verificarGerenciaUsuario(1L));
    }

    @Test
    @DisplayName("cliente le o proprio cadastro, os proprios telefones e os proprios veiculos")
    void clienteLeOProprioCadastro() {
        logar(2L, PerfilUsuario.CLIENTE);
        Usuario cliente = usuario(2L, PerfilUsuario.CLIENTE);
        Veiculo veiculo = new Veiculo();
        veiculo.setId(20L);
        veiculo.setProprietario(cliente);
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(cliente));
        when(usuarioRepositorio.findByTelefonesId(10L)).thenReturn(Optional.of(cliente));
        when(usuarioRepositorio.findByEnderecoId(30L)).thenReturn(Optional.of(cliente));
        when(veiculoRepositorio.findById(20L)).thenReturn(Optional.of(veiculo));

        assertDoesNotThrow(() -> {
            servico.verificarLeituraUsuario(2L);
            servico.verificarLeituraTelefone(10L);
            servico.verificarLeituraEndereco(30L);
            servico.verificarLeituraVeiculo(20L);
        });
        assertNega(() -> servico.verificarGerenciaVeiculo(20L));
    }

    @Test
    @DisplayName("cliente nao le outro usuario nem os sub-recursos dele")
    void clienteNaoLeOutroUsuario() {
        logar(2L, PerfilUsuario.CLIENTE);
        Usuario outro = usuario(8L, PerfilUsuario.CLIENTE);
        when(usuarioRepositorio.findById(8L)).thenReturn(Optional.of(outro));
        when(usuarioRepositorio.findByDocumentosId(11L)).thenReturn(Optional.of(outro));
        when(usuarioRepositorio.findByEmailsId(12L)).thenReturn(Optional.of(outro));
        when(usuarioRepositorio.findByCredenciaisId(13L)).thenReturn(Optional.of(outro));

        assertNega(() -> servico.verificarLeituraUsuario(8L));
        assertNega(() -> servico.verificarLeituraDocumento(11L));
        assertNega(() -> servico.verificarLeituraEmail(12L));
        assertNega(() -> servico.verificarLeituraCredencial(13L));
    }

    @Test
    @DisplayName("vendedor gerencia documento, e-mail e credencial de cliente, mas nao de gerente")
    void vendedorSubRecursosPorDono() {
        logar(1L, PerfilUsuario.VENDEDOR);
        Usuario cliente = usuario(2L, PerfilUsuario.CLIENTE);
        Usuario gerente = usuario(4L, PerfilUsuario.GERENTE);
        when(usuarioRepositorio.findByDocumentosId(11L)).thenReturn(Optional.of(cliente));
        when(usuarioRepositorio.findByEmailsId(12L)).thenReturn(Optional.of(cliente));
        when(usuarioRepositorio.findByCredenciaisId(13L)).thenReturn(Optional.of(gerente));

        assertDoesNotThrow(() -> servico.verificarGerenciaDocumento(11L));
        assertDoesNotThrow(() -> servico.verificarGerenciaEmail(12L));
        assertNega(() -> servico.verificarGerenciaCredencial(13L));
    }

    @Test
    @DisplayName("cliente le a venda em que e o cliente e nao le a de outro cliente")
    void clienteLeVendaPropria() {
        logar(2L, PerfilUsuario.CLIENTE);
        Usuario vendedor = usuario(1L, PerfilUsuario.VENDEDOR);
        when(vendaRepositorio.findById(1L))
                .thenReturn(Optional.of(venda(1L, usuario(2L, PerfilUsuario.CLIENTE), vendedor)));
        when(vendaRepositorio.findById(2L))
                .thenReturn(Optional.of(venda(2L, usuario(8L, PerfilUsuario.CLIENTE), vendedor)));

        assertDoesNotThrow(() -> servico.verificarLeituraVenda(1L));
        assertNega(() -> servico.verificarLeituraVenda(2L));
    }

    @Test
    @DisplayName("vendedor le a venda em que e o funcionario e nao le a de outro funcionario")
    void vendedorLeVendaPropria() {
        logar(1L, PerfilUsuario.VENDEDOR);
        Usuario cliente = usuario(2L, PerfilUsuario.CLIENTE);
        when(vendaRepositorio.findById(1L))
                .thenReturn(Optional.of(venda(1L, cliente, usuario(1L, PerfilUsuario.VENDEDOR))));
        when(vendaRepositorio.findById(2L))
                .thenReturn(Optional.of(venda(2L, cliente, usuario(6L, PerfilUsuario.VENDEDOR))));

        assertDoesNotThrow(() -> servico.verificarLeituraVenda(1L));
        assertNega(() -> servico.verificarLeituraVenda(2L));
    }

    @Test
    @DisplayName("gerente le qualquer venda sem consultar o repositorio")
    void gerenteLeQualquerVenda() {
        logar(4L, PerfilUsuario.GERENTE);

        assertDoesNotThrow(() -> servico.verificarLeituraVenda(2L));
        assertTrue(servico.podeLer(venda(2L, usuario(8L, PerfilUsuario.CLIENTE), usuario(6L, PerfilUsuario.VENDEDOR))));
        verificarSemConsultas();
    }

    @Test
    @DisplayName("vendedor cria venda feita por ele mesmo e nao cria venda de outro funcionario")
    void vendedorCriaSoVendaPropria() {
        logar(1L, PerfilUsuario.VENDEDOR);

        assertDoesNotThrow(() -> servico.verificarCriacaoVenda(1L));
        assertNega(() -> servico.verificarCriacaoVenda(6L));
        assertNega(() -> servico.verificarCriacaoVenda(null));
    }

    @Test
    @DisplayName("gerente cria venda de qualquer funcionario; cliente nao cria venda")
    void criacaoVendaGerenteECliente() {
        logar(4L, PerfilUsuario.GERENTE);
        assertDoesNotThrow(() -> servico.verificarCriacaoVenda(1L));

        logar(2L, PerfilUsuario.CLIENTE);
        assertNega(() -> servico.verificarCriacaoVenda(2L));
    }

    @Test
    @DisplayName("recurso inexistente nao nega: o servico da ATVIII responde 404")
    void recursoInexistenteNaoNega() {
        logar(2L, PerfilUsuario.CLIENTE);
        when(usuarioRepositorio.findById(50L)).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByDocumentosId(50L)).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByEmailsId(50L)).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByCredenciaisId(50L)).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByEnderecoId(50L)).thenReturn(Optional.empty());
        when(empresaRepositorio.findByEnderecoId(50L)).thenReturn(Optional.empty());
        when(veiculoRepositorio.findById(50L)).thenReturn(Optional.empty());
        when(vendaRepositorio.findById(50L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            servico.verificarLeituraUsuario(50L);
            servico.verificarGerenciaUsuario(50L);
            servico.verificarLeituraDocumento(50L);
            servico.verificarLeituraEmail(50L);
            servico.verificarLeituraCredencial(50L);
            servico.verificarLeituraEndereco(50L);
            servico.verificarLeituraVeiculo(50L);
            servico.verificarLeituraVenda(50L);
        });
    }

    @Test
    @DisplayName("telefone e endereco de empresa negam para gerente")
    void subRecursoDeEmpresaNegaParaGerente() {
        logar(4L, PerfilUsuario.GERENTE);
        when(usuarioRepositorio.findByTelefonesId(10L)).thenReturn(Optional.empty());
        when(empresaRepositorio.findByTelefonesId(10L)).thenReturn(Optional.of(new Empresa()));
        when(usuarioRepositorio.findByEnderecoId(30L)).thenReturn(Optional.empty());
        when(empresaRepositorio.findByEnderecoId(30L)).thenReturn(Optional.of(new Empresa()));

        assertNega(() -> servico.verificarLeituraTelefone(10L));
        assertNega(() -> servico.verificarGerenciaTelefone(10L));
        assertNega(() -> servico.verificarLeituraEndereco(30L));
        assertNega(() -> servico.verificarGerenciaEndereco(30L));
    }

    @Test
    @DisplayName("telefone sem dono nao nega")
    void telefoneSemDonoNaoNega() {
        logar(1L, PerfilUsuario.VENDEDOR);
        when(usuarioRepositorio.findByTelefonesId(10L)).thenReturn(Optional.empty());
        when(empresaRepositorio.findByTelefonesId(10L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servico.verificarGerenciaTelefone(10L));
        verify(empresaRepositorio).findByTelefonesId(10L);
    }

    @Test
    @DisplayName("filtrarUsuarios para gerente remove administrador e fornecedor e mantem ele mesmo")
    void filtrarUsuariosGerente() {
        logar(4L, PerfilUsuario.GERENTE);
        Usuario vendedor = usuario(1L, PerfilUsuario.VENDEDOR);
        Usuario fornecedor = usuario(2L, PerfilUsuario.FORNECEDOR);
        Usuario cliente = usuario(3L, PerfilUsuario.CLIENTE);
        Usuario administrador = usuario(5L, PerfilUsuario.ADMINISTRADOR);
        Usuario gerente = usuario(4L, PerfilUsuario.GERENTE);

        List<Usuario> filtrados = servico.filtrarUsuarios(
                List.of(vendedor, fornecedor, cliente, administrador, gerente));

        assertEquals(List.of(vendedor, cliente, gerente), filtrados);
        verificarSemConsultas();
    }

    @Test
    @DisplayName("filtrarUsuarios para vendedor mantem so os clientes e ele mesmo")
    void filtrarUsuariosVendedor() {
        logar(1L, PerfilUsuario.VENDEDOR);
        Usuario vendedor = usuario(1L, PerfilUsuario.VENDEDOR);
        Usuario fornecedor = usuario(2L, PerfilUsuario.FORNECEDOR);
        Usuario cliente = usuario(3L, PerfilUsuario.CLIENTE);
        Usuario gerente = usuario(4L, PerfilUsuario.GERENTE);
        Usuario outroVendedor = usuario(6L, PerfilUsuario.VENDEDOR);
        Usuario clienteVendedor = usuario(7L, PerfilUsuario.CLIENTE, PerfilUsuario.VENDEDOR);

        List<Usuario> filtrados = servico.filtrarUsuarios(
                List.of(vendedor, fornecedor, cliente, gerente, outroVendedor, clienteVendedor));

        assertEquals(List.of(vendedor, cliente), filtrados);
    }

    @Test
    @DisplayName("filtrarVendas para vendedor mantem so as vendas em que ele e o funcionario")
    void filtrarVendasVendedor() {
        logar(1L, PerfilUsuario.VENDEDOR);
        Usuario cliente = usuario(2L, PerfilUsuario.CLIENTE);
        Venda propria = venda(1L, cliente, usuario(1L, PerfilUsuario.VENDEDOR));
        Venda deOutro = venda(2L, cliente, usuario(6L, PerfilUsuario.VENDEDOR));
        Venda comoCliente = venda(3L, usuario(1L, PerfilUsuario.VENDEDOR), usuario(6L, PerfilUsuario.VENDEDOR));

        assertEquals(List.of(propria), servico.filtrarVendas(List.of(propria, deOutro, comoCliente)));
        verificarSemConsultas();
    }

    @Test
    @DisplayName("filtrarVendas para cliente mantem so as vendas em que ele e o cliente")
    void filtrarVendasCliente() {
        logar(2L, PerfilUsuario.CLIENTE);
        Usuario vendedor = usuario(1L, PerfilUsuario.VENDEDOR);
        Venda propria = venda(1L, usuario(2L, PerfilUsuario.CLIENTE), vendedor);
        Venda deOutro = venda(2L, usuario(8L, PerfilUsuario.CLIENTE), vendedor);
        Venda comoFuncionario = venda(3L, usuario(8L, PerfilUsuario.CLIENTE), usuario(2L, PerfilUsuario.CLIENTE));

        assertEquals(List.of(propria), servico.filtrarVendas(List.of(propria, deOutro, comoFuncionario)));
    }

    @Test
    @DisplayName("filtrarVendas para quem e vendedor e cliente usa a uniao das duas regras")
    void filtrarVendasVendedorECliente() {
        logar(1L, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE);
        Usuario eleMesmo = usuario(1L, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE);
        Venda comoFuncionario = venda(1L, usuario(2L, PerfilUsuario.CLIENTE), eleMesmo);
        Venda comoCliente = venda(2L, eleMesmo, usuario(6L, PerfilUsuario.VENDEDOR));
        Venda deOutros = venda(3L, usuario(2L, PerfilUsuario.CLIENTE), usuario(6L, PerfilUsuario.VENDEDOR));

        assertEquals(List.of(comoFuncionario, comoCliente),
                servico.filtrarVendas(List.of(comoFuncionario, comoCliente, deOutros)));
    }

    @Test
    @DisplayName("fornecedor autenticado nao gerencia nem le ninguem alem de si")
    void fornecedorNaoGerencia() {
        logar(9L, PerfilUsuario.FORNECEDOR);
        when(usuarioRepositorio.findById(2L)).thenReturn(Optional.of(usuario(2L, PerfilUsuario.CLIENTE)));

        assertNega(() -> servico.verificarLeituraUsuario(2L));
        assertNega(() -> servico.verificarCriacaoVenda(9L));
        Usuario fornecedor = usuario(9L, PerfilUsuario.FORNECEDOR);
        assertFalse(servico.podeLer(venda(1L, fornecedor, fornecedor)));
    }
}
