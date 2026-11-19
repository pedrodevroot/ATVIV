package com.autobots.automanager.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProvedorJwtTest {

    private static final String SEGREDO = "segredo-de-teste-do-automanager-com-pelo-menos-sessenta-e-quatro-caracteres";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-do-automanager-com-mais-de-sessenta-e-quatro-caracteres";
    private static final long DURACAO = 60000;

    private final ProvedorJwt provedor = new ProvedorJwt(SEGREDO, DURACAO);

    @Test
    @DisplayName("proverJwt gera um token com tres partes")
    void gerarTokenComTresPartes() {
        String jwt = provedor.proverJwt("ana");

        assertNotNull(jwt);
        assertEquals(3, jwt.split("\\.").length);
    }

    @Test
    @DisplayName("validarJwt aceita o token gerado pelo proprio provedor")
    void validarTokenGerado() {
        assertTrue(provedor.validarJwt(provedor.proverJwt("ana")));
    }

    @Test
    @DisplayName("obterNomeUsuario devolve o nome gravado no token")
    void obterNomeUsuario() {
        assertEquals("ana", provedor.obterNomeUsuario(provedor.proverJwt("ana")));
    }

    @Test
    @DisplayName("obterExpiracao fica perto de agora mais a duracao")
    void obterExpiracao() {
        long antes = System.currentTimeMillis();
        Date expiracao = provedor.obterExpiracao(provedor.proverJwt("ana"));
        long depois = System.currentTimeMillis();

        assertNotNull(expiracao);
        assertTrue(expiracao.getTime() >= antes + DURACAO - 1000);
        assertTrue(expiracao.getTime() <= depois + DURACAO + 1000);
    }

    @Test
    @DisplayName("token expirado e invalido")
    void tokenExpiradoInvalido() {
        ProvedorJwt expirado = new ProvedorJwt(SEGREDO, -1000);
        String jwt = expirado.proverJwt("ana");

        assertFalse(provedor.validarJwt(jwt));
        assertNull(provedor.obterNomeUsuario(jwt));
        assertNull(provedor.obterExpiracao(jwt));
    }

    @Test
    @DisplayName("token assinado com outro segredo e invalido")
    void tokenDeOutroSegredoInvalido() {
        String jwt = new ProvedorJwt(OUTRO_SEGREDO, DURACAO).proverJwt("ana");

        assertFalse(provedor.validarJwt(jwt));
        assertNull(provedor.obterNomeUsuario(jwt));
    }

    @Test
    @DisplayName("token adulterado e invalido")
    void tokenAdulteradoInvalido() {
        String jwt = provedor.proverJwt("ana");
        String[] partes = jwt.split("\\.");
        String outraCarga = new ProvedorJwt(SEGREDO, DURACAO).proverJwt("intruso").split("\\.")[1];
        String adulterado = partes[0] + "." + outraCarga + "." + partes[2];

        assertFalse(provedor.validarJwt(adulterado));
        assertFalse(provedor.validarJwt(jwt.substring(0, jwt.length() - 2) + "xx"));
    }

    @Test
    @DisplayName("texto qualquer, vazio e nulo sao invalidos")
    void textosInvalidos() {
        assertFalse(provedor.validarJwt("isto-nao-e-um-jwt"));
        assertFalse(provedor.validarJwt(""));
        assertFalse(provedor.validarJwt("   "));
        assertFalse(provedor.validarJwt(null));
        assertNull(provedor.obterNomeUsuario(null));
        assertNull(provedor.obterExpiracao(null));
    }

    @Test
    @DisplayName("segredo com menos de 64 bytes lanca IllegalArgumentException")
    void segredoCurto() {
        assertThrows(IllegalArgumentException.class, () -> new ProvedorJwt("curto", DURACAO));
    }
}
