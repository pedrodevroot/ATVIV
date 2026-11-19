package com.autobots.automanager.filtros;

class ValidadorCabecalho {
    static final String PREFIXO = "Bearer ";

    private final String cabecalho;

    ValidadorCabecalho(String cabecalho) {
        this.cabecalho = cabecalho;
    }

    boolean validar() {
        return cabecalho != null && cabecalho.startsWith(PREFIXO);
    }
}
