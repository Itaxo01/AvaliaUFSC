package com.example.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.model.Avaliacao;
import com.example.model.Comentario;
import com.example.model.Disciplina;
import com.example.model.Professor;
import com.example.model.Usuario;
import com.example.DTO.AvaliacaoDTO;
import com.example.repository.AvaliacaoRepository;

@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private DisciplinaService disciplinaService;

    @Autowired
    private ProfessorService professorService;

	 private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AvaliacaoService.class);
    // Criar nova avaliação
    public Avaliacao salvar(Avaliacao avaliacao) {
		  logger.debug("A");
        Avaliacao avaliacaoSalva = avaliacaoRepository.save(avaliacao);

		  logger.debug("Avaliação salva com ID: " + avaliacaoSalva.getId());
        
        return avaliacaoSalva;
    }

    // Buscar avaliação por ID
    public Optional<Avaliacao> buscarPorId(Long id) {
        return avaliacaoRepository.findById(id);
    }

    public Optional<Avaliacao> buscarPorUsuarioDisciplina(Usuario user, Disciplina disciplina) {
        if (user == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (disciplina == null) {
            throw new IllegalArgumentException("Disciplina não existe.");
        }

        return avaliacaoRepository.findByDisciplinaAndProfessorIsNullAndUsuario(disciplina, user);
    }

    public boolean possuiAvaliacaoPorUsuarioDisciplina(Usuario user, Disciplina disciplina) {
        return buscarPorUsuarioDisciplina(user, disciplina).isPresent();
    }

    // Buscar todas as avaliações
    public List<Avaliacao> buscarTodas() {
        return avaliacaoRepository.findAll();
    }

    // Deletar avaliação
    public void deletar(Long id) {
        avaliacaoRepository.deleteById(id);
    }

	 // Create na verdade cria ou edita avaliação (apenas nota)
    public Optional<Avaliacao> create(Professor professor, Disciplina disciplina, Usuario usuario, Integer nota){
        Optional<Avaliacao> avaliacaoExistente;
		  logger.debug("Criando ou atualizando avaliação para usuário: " + usuario.getEmail() +
			  		   ", disciplina: " + disciplina.getCodigo() +
			  		   (professor != null ? ", professor: " + professor.getProfessorId() : ", avaliação da disciplina"));

        if(disciplina == null || usuario == null){
            throw new IllegalArgumentException("Disciplina e usuario não podem ser nulos para criar avaliação.");
        }
		  
		  if(nota == null || nota < 1 || nota > 5){
            throw new IllegalArgumentException("Nota deve estar entre 1 e 5.");
        }
		  
        if(professor == null){
            avaliacaoExistente = avaliacaoRepository.findByProfessorIsNullAndDisciplinaAndUsuario(disciplina, usuario);
        }else{
            avaliacaoExistente = avaliacaoRepository.findByProfessorAndDisciplinaAndUsuario(professor, disciplina, usuario);
        }
		  
		  logger.debug("Avaliação existente: " + avaliacaoExistente.isPresent());
	
        if(avaliacaoExistente.isPresent()){
            Avaliacao avaliacao = avaliacaoExistente.get();
            avaliacao.setNota(nota);
				logger.debug("Salvando avaliação atualizada.");
            return Optional.of(salvar(avaliacao));
        } else {
				logger.debug("Avaliação não encontrada, criando nova avaliação.");
            Avaliacao avaliacao = new Avaliacao(nota, professor, disciplina, usuario);
				return Optional.of(salvar(avaliacao));
        }
    }

    // Verificar se existe avaliação
    public boolean existe(@NonNull Long id) {
        return avaliacaoRepository.existsById(id);
    }

    // Contar total de avaliações
    public long contar() {
        return avaliacaoRepository.count();
    }


	 /* O front-end vai usar isso aqui e processar para criar a página da disciplina */
	@Transactional(readOnly = true)
	public List<AvaliacaoDTO> buscarTodasAvaliacoesDisciplina(String disciplinaId, String sessionUsuarioEmail) {
		Optional<Disciplina> disciplinaOpt = disciplinaService.buscarPorCodigo(disciplinaId);
		if (disciplinaOpt.isEmpty()) {
			throw new IllegalArgumentException("Disciplina não existe.");
		}
		;
		List<Avaliacao> avaliacoes = avaliacaoRepository.findAllAvaliacoesByDisciplina(disciplinaOpt.get());
		  
		return avaliacoes.stream()
			.map(a -> AvaliacaoDTO.from(a, sessionUsuarioEmail))
			.collect(Collectors.toList());
	}
}
