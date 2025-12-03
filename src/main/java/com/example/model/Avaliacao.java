package com.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidade JPA que representa uma avaliação (rating) cadastrada no sistema.
 * <p>Uma avaliação contém apenas a nota dada pelo usuário a uma disciplina ou professor.</p>
 * <p>Comentários agora são entidades independentes e não fazem mais parte da Avaliacao.</p>
 * <ul>
 *   <li>{@link #nota} - Nota dada pelo usuário (1 a 5). Não pode ser nulo.</li>
 *   <li>{@link #professor} - ID do professor avaliado. Pode ser nulo se a avaliação for apenas da disciplina</li>
 *   <li>{@link #disciplina} - Código da disciplina avaliada</li>
 *   <li>{@link #usuario} - Email do usuário que fez a avaliação</li>
 *   <li>{@link #createdAt} - Timestamp de quando a avaliação foi criada</li>
 * </ul>
 */
@Entity
@Table(
	name = "avaliacoes",
	indexes = {
		@Index(name="uniqueTupleDisplinaUsusuarioProfessor" ,columnList = "disciplina_id, user_email, professor_id", unique = true)
	})
public class Avaliacao {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "professor_id")
	private Professor professor;

	@ManyToOne
	@JoinColumn(name = "disciplina_id")
	private Disciplina disciplina;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
	private Usuario usuario;

	@Column(name = "nota", nullable = false)
	private Integer nota;

	@Column(name = "created_at")
	private java.time.Instant createdAt = java.time.Instant.now();

	/** Construtor completo. */
	public Avaliacao(Integer nota, Professor professor, Disciplina disciplina, Usuario usuario) {
		this.nota = nota;
		this.professor = professor;
		this.disciplina = disciplina;
		this.usuario = usuario;
	}

	/** Construtor padrão necessário para JPA. */
	public Avaliacao(){}

		// Getters and Setters
	public Long getId() { 
		return id; 
	}
	
	public void setId(Long id) { 
		this.id = id; 
	}

	public Professor getProfessor() { return professor; }
	public void setProfessorId(Professor professor) { this.professor = professor; }

	public Disciplina getDisciplina() { return disciplina; }
	public void setDisciplinaId(Disciplina disciplina) { this.disciplina = disciplina; }

	public Usuario getUsuario() { return usuario; }
	public void setUserEmail(Usuario usuario) { this.usuario = usuario; }

	public Integer getNota() { return nota; }
	public void setNota(Integer nota) { this.nota = nota; }

	public java.time.Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }

	@Override
	public String toString() {
		return "Avaliacao{" +
				"id=" + id +
				", nota=" + nota +
				", professorId='" + (professor != null ? professor.getProfessorId() : "null") + '\'' +
				", disciplinaId='" + disciplina.getDisciplinaId() + '\'' +
				", userEmail='" + usuario.getEmail() + '\'' +
				", createdAt=" + createdAt +
				'}';
	}
}