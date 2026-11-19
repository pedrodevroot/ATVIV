package com.autobots.automanager.configuracao;

public final class Autorizacoes {
    public static final String ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    public static final String ADMINISTRADOR_GERENTE = "hasAnyRole('ADMINISTRADOR','GERENTE')";
    public static final String FUNCIONARIOS = "hasAnyRole('ADMINISTRADOR','GERENTE','VENDEDOR')";
    public static final String PERFIS_DE_ACESSO = "hasAnyRole('ADMINISTRADOR','GERENTE','VENDEDOR','CLIENTE')";

    private Autorizacoes() {
    }
}
