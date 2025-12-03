package com.example.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidade de domínio que representa um comentário feito por um usuário em uma avaliação.
 * <p>Um comentário pode ter arquivos anexados e pode ser uma resposta a outro comentário.</p>
 * <p>Um comentário pode ter várias respostas, formando uma árvore de comentários.</p>
 * <ul>
 * <li>{@link #usuario} Usuário que fez o comentário.</li>
 * <li>{@link #texto} Texto do comentário.</li>
 * <li>{@link #createdAt} Timestamp de quando o comentário foi criado.</li>
 * <li>{@link #avaliacao} Avaliação à qual o comentário principal está associado (pode ser nulo para respostas).</li>
 * <li>{@link #arquivos} Lista de arquivos anexados ao comentário.</li>
 * <li>{@link #pai} Comentário pai, se este comentário for uma resposta.</li>
 * <li>{@link #filhos} Lista de respostas a este comentário.</li>
 * <li>{@link #upVotes} Número de votos positivos no comentário.</li>
 * <li>{@link #downVotes} Número de votos negativos no comentário.</li>
 * </ul>
 */

@Entity
@Table(name = "comentarios", indexes = {
	@Index(name = "idx_pai_id", columnList = "pai_id"),
	@Index(name = "idx_user_created", columnList = "user_email, created_at"),
	@Index(name = "idx_created", columnList = "created_at")
})

public class Comentario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long comentarioId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_email", nullable = false)
	private Usuario usuario;

	@Column(name = "up_votes")
	private Integer upVotes = 0;

	@Column(name = "down_votes")
	private Integer downVotes = 0;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "comentario_votes",
		joinColumns = @JoinColumn(name = "comentario_id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"comentario_id", "user_email"})
	)
	@MapKeyColumn(name = "user_email")
	@Column(name = "is_upvote")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Map<String, Boolean> votes = new HashMap<>();

	@Column(nullable = false, length = 2000)
	private String texto;

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	@Column(name = "is_edited", nullable = false)
	private Boolean isEdited = false;

	@Column(name = "edited_at")
	private Instant editedAt;

	// ✅ Campos para sistema de revisão de comentários
	@Column(name = "alarmante", nullable = false)
	private Boolean alarmante = false;

	@Column(name = "denunciado", nullable = false)
	private Boolean denunciado = false;

	@Column(name = "denuncias_count")
	private Integer denunciasCount = 0;

	// ✅ Conjunto de usuários que denunciaram este comentário (para evitar duplicatas)
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "comentario_denuncias",
		joinColumns = @JoinColumn(name = "comentario_id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"comentario_id", "user_email"})
	)
	@Column(name = "user_email")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<String> denunciadoPor = new HashSet<>();


	// ✅ Relacionamento direto com Disciplina e Professor (comentários agora são apenas para professores)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disciplina_id", nullable = false)
	private Disciplina disciplina;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "professor_id", nullable = false)
	private Professor professor;

	@OneToMany(mappedBy = "comentario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<ArquivoComentario> arquivos = new ArrayList<>();

	// Relacionamento autoreferencial - comentário pai
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pai_id")
	private Comentario pai;

	// Relacionamento autoreferencial - comentários filhos
	@OneToMany(mappedBy = "pai", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<Comentario> filhos = new ArrayList<>();

	public Comentario(){}

	/** Construtor para comentário principal (raiz) */
	public Comentario(Usuario usuario, String texto, Disciplina disciplina, Professor professor) {
		this.usuario = usuario;
		this.texto = texto;
		this.disciplina = disciplina;
		this.professor = professor;
	}

	/** Construtor para comentário com pai (resposta) */
	public Comentario(Usuario usuario, String texto, Comentario pai) {
		this.usuario = usuario;
		this.texto = texto;
		pai.addFilho(this);
		// Herdar disciplina e professor do pai
		this.disciplina = pai.getDisciplina();
		this.professor = pai.getProfessor();
	}


	public Long getComentarioId() { return comentarioId; }
	public void setComentarioId(Long id) { this.comentarioId = id; }

	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }

	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public List<ArquivoComentario> getArquivos() { return arquivos; }
	public void setArquivos(List<ArquivoComentario> arquivos) { this.arquivos = arquivos; }

	public Comentario getPai() { return pai; }
	public void setPai(Comentario pai) { this.pai = pai; }

	public List<Comentario> getFilhos() { return filhos; }
	public void setFilhos(List<Comentario> filhos) { this.filhos = filhos; }

	public Disciplina getDisciplina() { return disciplina; }
	public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }

	public Professor getProfessor() { return professor; }
	public void setProfessor(Professor professor) { this.professor = professor; }

	public Integer getUpVotes() { return upVotes; }
	public void setUpVotes(Integer upVotes) { this.upVotes = upVotes; }

	public Integer getDownVotes() { return downVotes; }
	public void setDownVotes(Integer downVotes) { this.downVotes = downVotes; }

	public Boolean getIsEdited() { return isEdited; }
	public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

	public Instant getEditedAt() { return editedAt; }
	public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

	public Boolean getAlarmante() { return alarmante; }
	public void setAlarmante(Boolean alarmante) { this.alarmante = alarmante; }

	public Boolean getDenunciado() { return denunciado; }
	public void setDenunciado(Boolean denunciado) { this.denunciado = denunciado; }

	public Integer getDenunciasCount() { return denunciasCount; }
	public void setDenunciasCount(Integer denunciasCount) { this.denunciasCount = denunciasCount; }

	public Set<String> getDenunciadoPor() { return denunciadoPor; }
	public void setDenunciadoPor(Set<String> denunciadoPor) { this.denunciadoPor = denunciadoPor; }

	/**
	 * Verifica se o usuário já denunciou este comentário
	 */
	public boolean foiDenunciadoPor(String userEmail) {
		return denunciadoPor != null && denunciadoPor.contains(userEmail);
	}

	public Integer hasVoted(String userEmail) {
		if(votes.containsKey(userEmail)){
			return votes.get(userEmail) ? 1 : -1;
		}
		return 0;
	}
	
	/**
	 * Retorna o voto do usuário: true (upvote), false (downvote), null (sem voto)
	 */
	public Boolean getUserVote(String userEmail) {
		return votes.get(userEmail);
	}
	
	/**
	 * Retorna o mapa de votos (para uso interno)
	 */
	public Map<String, Boolean> getVotes() {
		return votes;
	}


	// Métodos de conveniência
	public void addFilho(Comentario filho) {
		filhos.add(filho);
		filho.setPai(this);
	}

	public void addArquivo(ArquivoComentario arquivo) {
		arquivos.add(arquivo);
		arquivo.setComentario(this);
	}

	public void edit(String novoTexto) {
		this.texto = novoTexto;
		this.isEdited = true;
		this.editedAt = Instant.now();
	}

	public boolean isResposta() {
		return pai != null;
	}

	public boolean isComentarioPrincipal() {
		return pai == null;
	}

	public boolean isComentarioAninhado() {
		return pai != null;
	}

	public Comentario getComentarioRaiz() {
		Comentario atual = this;
		while (atual.getPai() != null) {
			atual = atual.getPai();
		}
		return atual;
	}

	public int getNivelProfundidade() {
		int nivel = 0;
		Comentario atual = this.pai;
		while (atual != null) {
			nivel++;
			atual = atual.getPai();
		}
		return nivel;
	}

	public int contarFilhosRecursivo() {
		int total = filhos.size();
		for (Comentario filho : filhos) {
			total += filho.contarFilhosRecursivo();
		}
		return total;
	}

	public boolean hasFilhos() {
		return filhos != null && !filhos.isEmpty();
	}

	public boolean hasArquivos() {
		return arquivos != null && !arquivos.isEmpty();
	}

	/**
	 * Verifica se o comentário é incomum (alarmante por natureza):
	 * - Mais de 150 caracteres
	 * - Possui arquivos anexados
	 */
	public boolean isIncomum() {
		return (texto != null && texto.length() > 150) || hasArquivos();
	}

	/**
	 * Marca o comentário como alarmante automaticamente se for incomum ou denunciado
	 */
	public void atualizarStatusAlarmante() {
		this.alarmante = isIncomum() || (denunciado != null && denunciado);
	}

	/**
	 * Adiciona uma denúncia ao comentário
	 * @param userEmail email do usuário que está denunciando
	 * @return true se a denúncia foi adicionada, false se o usuário já denunciou
	 */
	public boolean adicionarDenuncia(String userEmail) {
		if (userEmail == null || userEmail.isBlank()) {
			throw new IllegalArgumentException("Email do usuário é obrigatório para denunciar");
		}
		
		// Verificar se o usuário já denunciou este comentário
		if (denunciadoPor.contains(userEmail)) {
			return false; // Usuário já denunciou
		}
		
		// Adicionar denúncia
		denunciadoPor.add(userEmail);
		this.denunciasCount = denunciadoPor.size();
		this.denunciado = true;
		this.alarmante = true;
		return true;
	}

	/**
	 * Marca o comentário como seguro (remove da lista de alarmantes)
	 */
	public void marcarComoSeguro() {
		this.alarmante = false;
		this.denunciado = false;
		this.denunciasCount = 0;
		this.denunciadoPor.clear();
	}

	public void addUserVote(String userEmail, Boolean isUpVote) throws Exception {
		if(isUpVote == null) {
			throw new IllegalArgumentException("isUpVote não pode ser nulo.");
		}
		Boolean votoExistente = votes.get(userEmail);
		if(votoExistente != null) {
			if(votoExistente.equals(isUpVote)) {
				if(isUpVote) {
					this.upVotes--;
				} else {
					this.downVotes--;
				}
				votes.remove(userEmail);
			} else {
				if(isUpVote) {
					this.upVotes++;
					this.downVotes--;
				} else {
					this.downVotes++;
					this.upVotes--;
				}
				votes.put(userEmail, isUpVote);
			}
		} else {
			if(isUpVote) {
				this.upVotes++;
			} else {
				this.downVotes++;
			}
			votes.put(userEmail, isUpVote);
		}
	}
}
