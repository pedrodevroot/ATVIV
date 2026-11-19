package com.autobots.automanager.dtos;

import java.util.Date;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TokenDto extends RepresentationModel<TokenDto> {
    private String tipo;
    private String token;
    private Date expiracao;
    private Long usuarioId;
}
