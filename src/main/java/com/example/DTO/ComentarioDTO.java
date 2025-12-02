package com.example.DTO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.util.List;

import com.example.model.Comentario;

public record ComentarioDTO (
		  Long id,
		  String texto,
		  Integer upVotes,
		  String professorId,
		  Integer downVotes,
		  Instant createdAt,
		  Boolean isOwner,
		  Integer hasVoted,
		  Boolean edited,
		  Instant editedAt,
		  Boolean deleted,
		  Long comentarioPaiId,
		  String userInitials,
		  Boolean alarmante,
		  Boolean denunciado,
		  Integer denunciasCount,
		  List<ArquivoDTO> arquivos,
		  List<ComentarioDTO> filhos) { 

	/**
	 * Extrai as iniciais do email do usuário (ex: "joao@email.com" -> "JO")
	 * Pega as duas primeiras letras do email antes do @
	 */
	private static String extractInitials(String email) {
		if (email == null || email.isBlank()) {
			return "?";
		}
		// Pegar a parte antes do @
		String localPart = email.split("@")[0];
		if (localPart.length() >= 2) {
			return localPart.substring(0, 2).toUpperCase();
		} else if (localPart.length() == 1) {
			return localPart.substring(0, 1).toUpperCase();
		}
		return "?";
	}

	public static ComentarioDTO from(Comentario c, String currentUserEmail) {
		String initials = c.getUsuario() != null ? extractInitials(c.getUsuario().getEmail()) : "?";
		
		return new ComentarioDTO(
				c.getComentarioId(),
				c.getTexto(),
				c.getUpVotes(),
				c.getProfessor() != null ? c.getProfessor().getProfessorId() : null,
				c.getDownVotes(),
				c.getCreatedAt(),
				c.getUsuario() != null ? c.getUsuario().getEmail().equals(currentUserEmail) : false,
				c.getUsuario() != null ? c.hasVoted(currentUserEmail) : 0,
				c.getIsEdited(),
				c.getEditedAt(),
				false,
				c.getPai() != null ? c.getPai().getComentarioId() : null,
				initials,
				c.getAlarmante(),
				c.getDenunciado(),
				c.getDenunciasCount(),
				c.getArquivos() != null ? c.getArquivos().stream()
						.map(arquivo -> ArquivoDTO.from(arquivo))
						.toList() : List.of(),
				c.getFilhos() != null ? c.getFilhos().stream()
						.map(filho -> ComentarioDTO.from(filho, currentUserEmail))
						.toList() : List.of()
		);
	}
	public String getDataFormatada() {
		return createdAt.atZone(ZoneId.systemDefault())
							.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR")));
	}
}