package com.autobots.automanager.excecoes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RespostaSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String NAO_AUTENTICADO =
            "Autenticacao necessaria: envie o header Authorization com um token JWT valido no formato Bearer <token>";
    public static final String ACESSO_NEGADO =
            "O perfil do usuario autenticado nao tem permissao para esta operacao";

    private final ObjectMapper mapeador;

    public RespostaSeguranca(ObjectMapper mapeador) {
        this.mapeador = mapeador;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
            AuthenticationException excecao) throws IOException {
        resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
            AccessDeniedException excecao) throws IOException {
        escrever(requisicao, resposta, HttpStatus.FORBIDDEN, ACESSO_NEGADO);
    }

    private void escrever(HttpServletRequest requisicao, HttpServletResponse resposta, HttpStatus status,
            String mensagem) throws IOException {
        ErroResposta corpo = new ErroResposta(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                requisicao.getRequestURI(),
                null);
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapeador.writeValue(resposta.getOutputStream(), corpo);
    }
}
