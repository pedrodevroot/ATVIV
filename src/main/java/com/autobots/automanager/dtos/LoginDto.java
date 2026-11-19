package com.autobots.automanager.dtos;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = "O nome de usuario e obrigatorio")
    @Size(max = 255, message = "O nome de usuario deve ter no maximo 255 caracteres")
    private String nomeUsuario;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(max = 72, message = "A senha deve ter no maximo 72 caracteres")
    private String senha;
}
