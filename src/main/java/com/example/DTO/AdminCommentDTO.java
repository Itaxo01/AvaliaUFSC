package com.example.DTO;

import java.time.Instant;
import java.util.List;

import com.example.model.Comentario;

/**
 * DTO para exibição de comentários no painel de administração.
 * Inclui informações completas sobre o usuário, disciplina e professor.
 */
public record AdminCommentDTO(
		Long id,
		String texto,
		Instant createdAt,
		Boolean alarmante,
		Boolean denunciado,
		Integer denunciasCount,
		Boolean edited,
		Instant editedAt,
		Integer upVotes,
		Integer downVotes,
		// Informações do usuário
		String userEmail,
		String userName,
		String userMatricula,
		String userInitials,
		// Informações da disciplina
		Long disciplinaId,
		String disciplinaNome,
		// Informações do professor
		String professorId,
		String professorNome,
		// Arquivos
		List<ArquivoDTO> arquivos,
		// Número de respostas
		Integer respostasCount
) {

	/**
	 * Extrai as iniciais do nome do usuário
	 */
	private static String extractInitials(String nome) {
		if (nome == null || nome.isBlank()) {
			return "?";
		}
		String[] partes = nome.trim().split("\\s+");
		if (partes.length >= 2) {
			return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
		} else if (partes.length == 1 && partes[0].length() >= 2) {
			return partes[0].substring(0, 2).toUpperCase();
		} else if (partes.length == 1) {
			return partes[0].substring(0, 1).toUpperCase();
		}
		return "?";
	}

	public static AdminCommentDTO from(Comentario c) {
		String initials = c.getUsuario() != null ? extractInitials(c.getUsuario().getNome()) : "?";
		
		return new AdminCommentDTO(
				c.getComentarioId(),
				c.getTexto(),
				c.getCreatedAt(),
				c.getAlarmante() != null ? c.getAlarmante() : false,
				c.getDenunciado() != null ? c.getDenunciado() : false,
				c.getDenunciasCount() != null ? c.getDenunciasCount() : 0,
				c.getIsEdited(),
				c.getEditedAt(),
				c.getUpVotes() != null ? c.getUpVotes() : 0,
				c.getDownVotes() != null ? c.getDownVotes() : 0,
				// Usuário
				c.getUsuario() != null ? c.getUsuario().getEmail() : null,
				c.getUsuario() != null ? c.getUsuario().getNome() : "Usuário removido",
				c.getUsuario() != null ? c.getUsuario().getMatricula() : null,
				initials,
				// Disciplina
				c.getDisciplina() != null ? c.getDisciplina().getDisciplinaId() : null,
				c.getDisciplina() != null ? c.getDisciplina().getNome() : null,
				// Professor
				c.getProfessor() != null ? c.getProfessor().getProfessorId() : null,
				c.getProfessor() != null ? c.getProfessor().getNome() : null,
				// Arquivos
				c.getArquivos() != null ? c.getArquivos().stream()
						.map(ArquivoDTO::from)
						.toList() : List.of(),
				// Respostas
				c.getFilhos() != null ? c.contarFilhosRecursivo() : 0
		);
	}
}
