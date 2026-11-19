package com.autobots.automanager.servicos;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.entidades.Veiculo;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.enumeracoes.PerfilUsuario;
import com.autobots.automanager.excecoes.RespostaSeguranca;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.repositorios.VeiculoRepositorio;
import com.autobots.automanager.repositorios.VendaRepositorio;

@Service
public class AutorizacaoServico {

    private static final String PREFIXO_PERFIL = "ROLE_";

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private EmpresaRepositorio empresaRepositorio;
    @Autowired
    private VeiculoRepositorio veiculoRepositorio;
    @Autowired
    private VendaRepositorio vendaRepositorio;

    public Set<PerfilUsuario> perfisDoUsuarioLogado() {
        Set<PerfilUsuario> perfis = EnumSet.noneOf(PerfilUsuario.class);
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || autenticacao.getAuthorities() == null) {
            return perfis;
        }
        for (GrantedAuthority autoridade : autenticacao.getAuthorities()) {
            converterAutoridade(autoridade).ifPresent(perfis::add);
        }
        return perfis;
    }

    public Long idDoUsuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) autenticacao.getPrincipal()).getUsuarioId();
        }
        return null;
    }

    public boolean ehAdministrador() {
        return perfisDoUsuarioLogado().contains(PerfilUsuario.ADMINISTRADOR);
    }

    public Set<PerfilUsuario> perfisGerenciaveis() {
        Set<PerfilUsuario> perfisLogado = perfisDoUsuarioLogado();
        Set<PerfilUsuario> gerenciaveis = EnumSet.noneOf(PerfilUsuario.class);
        if (perfisLogado.contains(PerfilUsuario.ADMINISTRADOR)) {
            gerenciaveis.addAll(EnumSet.allOf(PerfilUsuario.class));
        }
        if (perfisLogado.contains(PerfilUsuario.GERENTE)) {
            gerenciaveis.addAll(EnumSet.of(PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE));
        }
        if (perfisLogado.contains(PerfilUsuario.VENDEDOR)) {
            gerenciaveis.add(PerfilUsuario.CLIENTE);
        }
        return gerenciaveis;
    }

    public boolean podeGerenciar(Usuario alvo) {
        if (ehAdministrador()) {
            return true;
        }
        Set<PerfilUsuario> perfisAlvo = alvo.getPerfis();
        return perfisAlvo != null && !perfisAlvo.isEmpty() && perfisGerenciaveis().containsAll(perfisAlvo);
    }

    public boolean podeLer(Usuario alvo) {
        if (podeGerenciar(alvo)) {
            return true;
        }
        Long logado = idDoUsuarioLogado();
        return logado != null && logado.equals(alvo.getId());
    }

    public boolean podeLer(Venda venda) {
        Set<PerfilUsuario> perfis = perfisDoUsuarioLogado();
        if (leTodasAsVendas(perfis)) {
            return true;
        }
        Long logado = idDoUsuarioLogado();
        if (logado == null) {
            return false;
        }
        if (perfis.contains(PerfilUsuario.VENDEDOR) && mesmoUsuario(venda.getFuncionario(), logado)) {
            return true;
        }
        return perfis.contains(PerfilUsuario.CLIENTE) && mesmoUsuario(venda.getCliente(), logado);
    }

    public void verificarLeituraUsuario(long usuarioId) {
        if (ehAdministrador()) {
            return;
        }
        usuarioRepositorio.findById(usuarioId).filter(usuario -> !podeLer(usuario)).ifPresent(usuario -> negar());
    }

    public void verificarGerenciaUsuario(long usuarioId) {
        if (ehAdministrador()) {
            return;
        }
        usuarioRepositorio.findById(usuarioId).filter(usuario -> !podeGerenciar(usuario))
                .ifPresent(usuario -> negar());
    }

    public void verificarPerfisAtribuiveis(Collection<PerfilUsuario> perfis) {
        if (ehAdministrador()) {
            return;
        }
        if (perfis == null || perfis.isEmpty() || !perfisGerenciaveis().containsAll(perfis)) {
            negar();
        }
    }

    public List<Usuario> filtrarUsuarios(List<Usuario> usuarios) {
        if (ehAdministrador()) {
            return usuarios;
        }
        return usuarios.stream().filter(this::podeLer).collect(Collectors.toList());
    }

    public void verificarLeituraTelefone(long id) {
        verificarTelefone(id, false);
    }

    public void verificarGerenciaTelefone(long id) {
        verificarTelefone(id, true);
    }

    public void verificarLeituraDocumento(long id) {
        verificarDocumento(id, false);
    }

    public void verificarGerenciaDocumento(long id) {
        verificarDocumento(id, true);
    }

    public void verificarLeituraEmail(long id) {
        verificarEmail(id, false);
    }

    public void verificarGerenciaEmail(long id) {
        verificarEmail(id, true);
    }

    public void verificarLeituraCredencial(long id) {
        verificarCredencial(id, false);
    }

    public void verificarGerenciaCredencial(long id) {
        verificarCredencial(id, true);
    }

    public void verificarLeituraEndereco(long id) {
        verificarEndereco(id, false);
    }

    public void verificarGerenciaEndereco(long id) {
        verificarEndereco(id, true);
    }

    public void verificarLeituraVeiculo(long id) {
        verificarVeiculo(id, false);
    }

    public void verificarGerenciaVeiculo(long id) {
        verificarVeiculo(id, true);
    }

    public void verificarLeituraVenda(long vendaId) {
        if (leTodasAsVendas(perfisDoUsuarioLogado())) {
            return;
        }
        vendaRepositorio.findById(vendaId).filter(venda -> !podeLer(venda)).ifPresent(venda -> negar());
    }

    public List<Venda> filtrarVendas(List<Venda> vendas) {
        if (leTodasAsVendas(perfisDoUsuarioLogado())) {
            return vendas;
        }
        return vendas.stream().filter(this::podeLer).collect(Collectors.toList());
    }

    public void verificarCriacaoVenda(Long funcionarioId) {
        Set<PerfilUsuario> perfis = perfisDoUsuarioLogado();
        if (leTodasAsVendas(perfis)) {
            return;
        }
        Long logado = idDoUsuarioLogado();
        boolean vendaPropria = perfis.contains(PerfilUsuario.VENDEDOR) && logado != null
                && logado.equals(funcionarioId);
        if (!vendaPropria) {
            negar();
        }
    }

    private void verificarTelefone(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(usuarioRepositorio.findByTelefonesId(id),
                () -> empresaRepositorio.findByTelefonesId(id).isPresent(), gerencia);
    }

    private void verificarDocumento(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(usuarioRepositorio.findByDocumentosId(id), () -> false, gerencia);
    }

    private void verificarEmail(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(usuarioRepositorio.findByEmailsId(id), () -> false, gerencia);
    }

    private void verificarCredencial(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(usuarioRepositorio.findByCredenciaisId(id), () -> false, gerencia);
    }

    private void verificarEndereco(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(usuarioRepositorio.findByEnderecoId(id),
                () -> empresaRepositorio.findByEnderecoId(id).isPresent(), gerencia);
    }

    private void verificarVeiculo(long id, boolean gerencia) {
        if (ehAdministrador()) {
            return;
        }
        verificarDono(veiculoRepositorio.findById(id).map(Veiculo::getProprietario), () -> false, gerencia);
    }

    private void verificarDono(Optional<Usuario> donoUsuario, BooleanSupplier donoEmpresa, boolean gerencia) {
        if (donoUsuario.isPresent()) {
            Usuario dono = donoUsuario.get();
            boolean permitido = gerencia ? podeGerenciar(dono) : podeLer(dono);
            if (!permitido) {
                negar();
            }
            return;
        }
        if (donoEmpresa.getAsBoolean()) {
            negar();
        }
    }

    private boolean leTodasAsVendas(Set<PerfilUsuario> perfis) {
        return perfis.contains(PerfilUsuario.ADMINISTRADOR) || perfis.contains(PerfilUsuario.GERENTE);
    }

    private boolean mesmoUsuario(Usuario usuario, Long id) {
        return usuario != null && id.equals(usuario.getId());
    }

    private Optional<PerfilUsuario> converterAutoridade(GrantedAuthority autoridade) {
        String nome = autoridade == null ? null : autoridade.getAuthority();
        if (nome == null || !nome.startsWith(PREFIXO_PERFIL)) {
            return Optional.empty();
        }
        String perfil = nome.substring(PREFIXO_PERFIL.length());
        for (PerfilUsuario valor : PerfilUsuario.values()) {
            if (valor.name().equals(perfil)) {
                return Optional.of(valor);
            }
        }
        return Optional.empty();
    }

    private void negar() {
        throw new AccessDeniedException(RespostaSeguranca.ACESSO_NEGADO);
    }
}
