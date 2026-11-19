package com.autobots.automanager.modelo;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.stereotype.Component;

import com.autobots.automanager.controles.UsuarioControle;
import com.autobots.automanager.dtos.TokenDto;

@Component
public class AdicionadorLinkToken implements AdicionadorLink<TokenDto> {

	@Override
	public void adicionarLink(List<TokenDto> lista) {
		for (TokenDto token : lista) {
			adicionarLink(token);
		}
	}

	@Override
	public void adicionarLink(TokenDto objeto) {
		long usuarioId = objeto.getUsuarioId();
		objeto.add(linkTo(methodOn(UsuarioControle.class).obterUsuario(usuarioId)).withRel("usuario"));
	}
}
