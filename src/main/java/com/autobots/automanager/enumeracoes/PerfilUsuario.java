package com.autobots.automanager.enumeracoes;

public enum PerfilUsuario {
	ADMINISTRADOR, GERENTE, VENDEDOR, CLIENTE, FORNECEDOR;

	public boolean ehFuncionario() {
		return this == ADMINISTRADOR || this == GERENTE || this == VENDEDOR;
	}
}
