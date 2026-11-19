package com.autobots.automanager.dtos;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class CredencialAtualizacaoDto {

    private Boolean inativo;

    @Size(max = 255, message = "O nome de usuario deve ter no maximo 255 caracteres")
    private String nomeUsuario;

    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "A senha nao pode conter apenas espacos")
    private String senha;

    @Positive(message = "O codigo de barras deve ser um numero positivo")
    private Long codigo;
}
