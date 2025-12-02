package com.example.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.factory.ComentarioFactory;
import com.example.model.Comentario;
import com.example.model.Disciplina;
import com.example.model.Professor;
import com.example.model.Usuario;
import com.example.repository.ComentarioRepository;

import com.example.DTO.ComentarioDTO;	


@Service
public class ComentarioService {
    
    @Autowired
    private ComentarioRepository comentarioRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	 public void delete(Comentario comentario) {
		  comentarioRepository.delete(comentario);
	 }

    // ✅ Criar comentário principal (com disciplina e professor)
    public Comentario criarComentario(Usuario usuario, String texto, Disciplina disciplina, Professor professor) {
        Comentario comentario = ComentarioFactory.criarComentario(usuario, texto, disciplina, professor, null);
        // Verificar se o comentário é incomum e marcar como alarmante se necessário
        comentario.atualizarStatusAlarmante();
        return comentarioRepository.save(comentario);
    }


	 public Comentario edit(Comentario comentario, String novoTexto) {
		  comentario.edit(novoTexto);
		  return comentarioRepository.save(comentario);
	 }

	 public Comentario salvar(Comentario comentario) {
		  return comentarioRepository.save(comentario);
	 }
    
    // Responder comentário (herda disciplina/professor do pai)
	@Transactional
    public Comentario responderComentario(Usuario usuario, String texto, Long parentId) {
        Comentario parent = comentarioRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentário pai não encontrado"));

		Comentario resposta = ComentarioFactory.criarComentario(usuario, texto, null, null, parent);
		Comentario saved = comentarioRepository.save(resposta); 
		
		return saved;
    }
	 
	 // ✅ Buscar comentários de uma disciplina (sem professor)
	 @Transactional(readOnly = true)
	 public List<ComentarioDTO> buscarComentariosDisciplina(Disciplina disciplina, String sessionUsuarioEmail) {
		  List<Comentario> comentarios = comentarioRepository.findByDisciplinaAndProfessorIsNullAndPaiIsNull(disciplina);
		  return comentarios.stream()
		 		.map(c -> ComentarioDTO.from(c, sessionUsuarioEmail))
				.collect(Collectors.toList());
	 }
	 
	 // ✅ Buscar comentários de um professor
	 @Transactional(readOnly = true)
	 public List<ComentarioDTO> buscarComentariosProfessor(Disciplina disciplina, Professor professor, String sessionUsuarioEmail) {
		  List<Comentario> comentarios = comentarioRepository.findByDisciplinaAndProfessorAndPaiIsNull(disciplina, professor);
		  return comentarios.stream()
				.map(c -> ComentarioDTO.from(c, sessionUsuarioEmail))
				.collect(Collectors.toList());
	 }
    
    // Buscar comentário por ID
    public Optional<Comentario> buscarPorId(Long id) {
        return comentarioRepository.findById(id);
    }
    
    // Buscar todos os comentários
    public List<Comentario> buscarTodos() {
        return comentarioRepository.findAll();
    }

    public void deletar(Long id) {
        comentarioRepository.deleteById(id);
    }
    
    // Verificar se existe comentário
    public boolean existe(Long id) {
        return comentarioRepository.existsById(id);
    }

	 public void vote(String userEmail, Long comentarioId, Boolean isUpVote) throws Exception {
		  Comentario comentario = comentarioRepository.findById(comentarioId)
					 .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado"));
		  
		  comentario.addUserVote(userEmail, isUpVote);
		  
		  comentarioRepository.save(comentario);
	 }

	 // ==================== Métodos para Admin ====================

	 /**
	  * Buscar todos os comentários principais ordenados pelos mais recentes
	  */
	 @Transactional(readOnly = true)
	 public Page<Comentario> buscarTodosComentariosOrdenados(int page, int size) {
		  Pageable pageable = PageRequest.of(page, size);
		  return comentarioRepository.findAllOrderByCreatedAtDesc(pageable);
	 }

	 /**
	  * Buscar comentários alarmantes (para revisão)
	  */
	 @Transactional(readOnly = true)
	 public Page<Comentario> buscarComentariosAlarmantes(int page, int size) {
		  Pageable pageable = PageRequest.of(page, size);
		  return comentarioRepository.findAlarmantes(pageable);
	 }

	 /**
	  * Contar comentários alarmantes
	  */
	 @Transactional(readOnly = true)
	 public long contarAlarmantes() {
		  return comentarioRepository.countAlarmantes();
	 }

	 /**
	  * Contar total de comentários
	  */
	 @Transactional(readOnly = true)
	 public long contarTodosComentarios() {
		  return comentarioRepository.countComentariosPrincipais();
	 }

	 /**
	  * Marcar comentário como seguro (remove da lista de alarmantes)
	  */
	 @Transactional
	 public Comentario marcarComoSeguro(Long comentarioId) {
		  Comentario comentario = comentarioRepository.findById(comentarioId)
					 .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado"));
		  
		  comentario.marcarComoSeguro();
		  return comentarioRepository.save(comentario);
	 }

	 /**
	  * Denunciar um comentário
	  * @param comentarioId ID do comentário
	  * @param userEmail Email do usuário que está denunciando
	  * @return true se a denúncia foi registrada, false se o usuário já denunciou
	  */
	 @Transactional
	 public boolean denunciar(Long comentarioId, String userEmail) {
		  Comentario comentario = comentarioRepository.findById(comentarioId)
					 .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado"));
		  
		  boolean denunciaAdicionada = comentario.adicionarDenuncia(userEmail);
		  
		  if (denunciaAdicionada) {
				comentarioRepository.save(comentario);
		  }
		  
		  return denunciaAdicionada;
	 }

	 /**
	  * Obter usuário do comentário (para banimento)
	  */
	 @Transactional(readOnly = true)
	 public Usuario getUsuarioDoComentario(Long comentarioId) {
		  Comentario comentario = comentarioRepository.findById(comentarioId)
					 .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado"));
		  return comentario.getUsuario();
	 }
	 
	 
}