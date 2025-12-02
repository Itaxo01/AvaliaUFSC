package com.example.model;

import java.time.Instant;

import jakarta.persistence.*;

/**
 * Entidade que armazena as matrículas de usuários banidos.
 * Um usuário banido não pode criar uma nova conta.
 */
@Entity
@Table(name = "usuarios_banidos")
public class UsuarioBanido {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String matricula;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String nome;

	@Column(name = "banido_em", nullable = false)
	private Instant banidoEm = Instant.now();

	@Column(name = "banido_por")
	private String banidoPor;

	@Column(length = 500)
	private String motivo;

	public UsuarioBanido() {}

	public UsuarioBanido(String matricula, String email, String nome, String banidoPor, String motivo) {
		this.matricula = matricula;
		this.email = email;
		this.nome = nome;
		this.banidoPor = banidoPor;
		this.motivo = motivo;
	}

	// Getters e Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getMatricula() { return matricula; }
	public void setMatricula(String matricula) { this.matricula = matricula; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }

	public Instant getBanidoEm() { return banidoEm; }
	public void setBanidoEm(Instant banidoEm) { this.banidoEm = banidoEm; }

	public String getBanidoPor() { return banidoPor; }
	public void setBanidoPor(String banidoPor) { this.banidoPor = banidoPor; }

	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
}
