package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.model.UsuarioBanido;

/**
 * Repository para gerenciar usuários banidos.
 */
@Repository
public interface UsuarioBanidoRepository extends JpaRepository<UsuarioBanido, Long> {
	
	/**
	 * Verifica se uma matrícula está banida.
	 */
	boolean existsByMatricula(String matricula);

	/**
	 * Busca um usuário banido pela matrícula.
	 */
	Optional<UsuarioBanido> findByMatricula(String matricula);

	/**
	 * Remove o banimento de uma matrícula.
	 */
	void deleteByMatricula(String matricula);
}
