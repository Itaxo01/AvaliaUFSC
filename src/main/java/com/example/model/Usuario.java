package com.example.model;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;

/**
 * Entidade de domínio que representa um usuário cadastrado no sistema. Utilizada para o manejo dos dados
 * <ul>
 * <li> {@link #userEmail} Email do usuário (chave de login).</li>
 * <li> {@link #password} Hash da senha armazenado.</li>
 * <li> {@link #matricula} Matrícula estudantil (ID).</li>
 * <li> {@link #isAdmin} Indica se o usuário é um administrador do sistema.</li>
 * </ul>
 * 
 * Nota: Nome e curso foram removidos para garantir anonimato dos usuários.
 */
@Entity
@Table(name = "usuarios")
public class Usuario{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(nullable = false)
	private String password;

	@Column(nullable = false, unique = true)
	private String matricula;


	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<Avaliacao> avaliacoes = new HashSet<>();

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<MapaCurricular> mapaCurricular = new HashSet<>();

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<Comentario> comentarios = new HashSet<>();

	@Column(name = "is_admin")
	private boolean isAdmin = false;

	/**
	 * Construtor completo utilizado pelo serviço/repositório.
	 */
	public Usuario(String email, String password, String matricula) {
		this.email = email;
		this.password = password;
		this.matricula = matricula;
	}

	/** Construtor padrão necessário para (de)serialização JSON. */
	public Usuario(){}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	/** Email (chave de login). */
	public String getEmail() { return email;}
	public void setEmail(String email) { this.email = email; }

	/** Hash da senha armazenado. */
	// the password here is already hashed
	public String getPassword() { return password;}
	public void setPassword(String password) { this.password = password;}

	/** Matrícula estudantil (ID). */
	public String getMatricula() { return matricula;}
	public void setMatricula(String matricula) { this.matricula = matricula;} // não deve ser usado de fato

	public boolean getIsAdmin() { return isAdmin; }
	public void setIsAdmin(boolean isAdmin) { isAdmin = isAdmin; }

	public void toggleIsAdmin() { this.isAdmin = !this.isAdmin; }


	public Set<Avaliacao> getAvaliacoes() { return avaliacoes; }
	public void setAvaliacoes(Set<Avaliacao> avaliacoes) { this.avaliacoes = avaliacoes; }

	public Set<MapaCurricular> getMapaCurricular() { return mapaCurricular; }
	public void setMapaCurricular(Set<MapaCurricular> mapaCurricular) { this.mapaCurricular = mapaCurricular; }

	public Set<Comentario> getComentarios() { return comentarios; }
	public void setComentarios(Set<Comentario> comentarios) { this.comentarios = comentarios; }

}
