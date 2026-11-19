package com.autobots.automanager.configuracao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.autobots.automanager.adaptadores.UserDetailsServiceImpl;
import com.autobots.automanager.jwt.ProvedorJwt;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import com.autobots.automanager.repositorios.VeiculoRepositorio;
import com.autobots.automanager.repositorios.VendaRepositorio;
import com.autobots.automanager.servicos.AutorizacaoServico;

@TestConfiguration
@Import({ ConfiguracaoSenha.class, AutorizacaoServico.class })
public class SegurancaTesteConfiguracao {

    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private ProvedorJwt provedorJwt;
    @MockBean
    private UsuarioRepositorio usuarioRepositorio;
    @MockBean
    private EmpresaRepositorio empresaRepositorio;
    @MockBean
    private VeiculoRepositorio veiculoRepositorio;
    @MockBean
    private VendaRepositorio vendaRepositorio;
}
