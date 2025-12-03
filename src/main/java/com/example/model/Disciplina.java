package com.example.model;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidade JPA que representa uma disciplina cadastrada no sistema.
 * <ul>
 * <li> {@link #codigo} Código único da disciplina (ex: "INE5101").</li>
 * <li> {@link #nome} Nome da disciplina (ex: "Introdução à Ciência da Computação").</li>
 * </ul>
 */
@Entity
@Table(name = "disciplinas")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disciplinaId;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;
    
    @Column(nullable = false, length = 300)
    private String nome;
	 
    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProfessorDisciplina> professorDisciplinas = new HashSet<>();

    /**
     * Construtor completo utilizado pelo serviço/repositório.
     */
    public Disciplina(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

	 /** Construtor padrão necessário para JPA. */
    public Disciplina(){}

    // Getters and Setters
    public Long getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(Long id) { this.disciplinaId = id; }

    /** Código da disciplina. */
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    /** Nome da disciplina. */
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    /** Lista de relacionamentos professor-disciplina. */
    public Set<ProfessorDisciplina> getProfessorDisciplinas() { 
		return professorDisciplinas; 
	}
    public void setProfessorDisciplinas(Set<ProfessorDisciplina> professorDisciplinas) { 
		this.professorDisciplinas = professorDisciplinas != null ? professorDisciplinas : new HashSet<>();
	 }

    
    public void adicionarProfessor(Professor professor, String ultimoSemestre) {
		ProfessorDisciplina pd = new ProfessorDisciplina(professor, this, ultimoSemestre);
		professorDisciplinas.add(pd);
    }
    
    public void atualizarSemestreProfessor(Professor professor, String ultimoSemestre) {
		professorDisciplinas.stream()
			.filter(pd -> pd.getProfessor().equals(professor))
			.findFirst()
			.ifPresent(pd -> pd.setUltimoSemestre(ultimoSemestre));
    }

	 public boolean temProfessor(Professor professor) {
		if(professorDisciplinas == null) {
	 	  professorDisciplinas = new HashSet<>();
		}
		return professorDisciplinas.stream()
			.anyMatch(pd -> pd.getProfessor().equals(professor));
	}

	 /**
	  * Método auxiliar para obter a lista de professores desta disciplina.
	  * Converte o Set de ProfessorDisciplina para Set de Professor.
	  */
	 public Set<Professor> getProfessores() {
		if(professorDisciplinas == null) {
			return new HashSet<>();
		}
		return professorDisciplinas.stream()
			.map(ProfessorDisciplina::getProfessor)
			.collect(java.util.stream.Collectors.toSet());
	 }

    
	 @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Disciplina)) return false;
        Disciplina disciplina = (Disciplina) o;
        return java.util.Objects.equals(codigo, disciplina.codigo);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "Disciplina{" +
                "id=" + disciplinaId +
                ", codigo='" + codigo + '\'' +
                ", nome='" + nome + '\'' +
                '}';
    }
}
