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
	 * Extrai as iniciais do nome do usuário (ex: "João Silva" -> "JS")
	 */
	private static String extractInitials(String nome) {
		if (nome == null || nome.isBlank()) {
			return "?";
		}
		String[] partes = nome.trim().split("\\s+");
		if (partes.length >= 2) {
			// Primeira letra do primeiro nome + primeira letra do último nome
			return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
		} else if (partes.length == 1 && partes[0].length() >= 2) {
			// Se só tem um nome, pega as duas primeiras letras
			return partes[0].substring(0, 2).toUpperCase();
		} else if (partes.length == 1) {
			return partes[0].substring(0, 1).toUpperCase();
		}
		return "?";
	}

	public static ComentarioDTO from(Comentario c, String currentUserEmail) {
		String initials = c.getUsuario() != null ? extractInitials(c.getUsuario().getNome()) : "?";
		
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