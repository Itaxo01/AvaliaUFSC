package com.example.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.model.Comentario;
import com.example.model.Disciplina;
import com.example.model.Professor;

/**
 * Repository para Comentario - agora independente de Avaliacao.
 */
@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

	// ✅ Buscar comentários principais (sem pai) de uma disciplina (sem professor)
	@Query("SELECT c FROM Comentario c WHERE c.disciplina = :disciplina AND c.professor IS NULL AND c.pai IS NULL")
	List<Comentario> findByDisciplinaAndProfessorIsNullAndPaiIsNull(@Param("disciplina") Disciplina disciplina);
	
	// ✅ Buscar comentários principais (sem pai) de um professor específico
	@Query("SELECT c FROM Comentario c WHERE c.disciplina = :disciplina AND c.professor = :professor AND c.pai IS NULL")
	List<Comentario> findByDisciplinaAndProfessorAndPaiIsNull(@Param("disciplina") Disciplina disciplina, @Param("professor") Professor professor);

	// ✅ Buscar todos os comentários ordenados pelos mais recentes (para admin)
	@Query("SELECT c FROM Comentario c WHERE c.pai IS NULL ORDER BY c.createdAt DESC")
	Page<Comentario> findAllOrderByCreatedAtDesc(Pageable pageable);

	// ✅ Buscar comentários alarmantes (para revisão do admin)
	@Query("SELECT c FROM Comentario c WHERE c.alarmante = true AND c.pai IS NULL ORDER BY c.createdAt DESC")
	Page<Comentario> findAlarmantes(Pageable pageable);

	// ✅ Buscar comentários denunciados
	@Query("SELECT c FROM Comentario c WHERE c.denunciado = true AND c.pai IS NULL ORDER BY c.denunciasCount DESC, c.createdAt DESC")
	List<Comentario> findDenunciados();

	// ✅ Contar comentários alarmantes
	@Query("SELECT COUNT(c) FROM Comentario c WHERE c.alarmante = true AND c.pai IS NULL")
	long countAlarmantes();

	// ✅ Contar total de comentários principais
	@Query("SELECT COUNT(c) FROM Comentario c WHERE c.pai IS NULL")
	long countComentariosPrincipais();
}
