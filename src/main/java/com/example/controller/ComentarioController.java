package com.example.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.model.Professor;
import com.example.model.Usuario;
import com.example.model.Comentario;
import com.example.model.Disciplina;
import com.example.model.ArquivoComentario;
import com.example.service.ComentarioService;
import com.example.service.DisciplinaService;
import com.example.service.ProfessorService;
import com.example.service.ArquivoComentarioService;
import com.example.service.UsuarioService;
import com.example.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Exibe detalhes da disciplina via path parameter.
*/
@Controller
@RequestMapping("/api/comentario")
public class ComentarioController {
	@Autowired
	private SessionService sessionService;

	@Autowired 
	private UsuarioService userService;

	@Autowired
	private DisciplinaService disciplinaService;

	@Autowired
	private ArquivoComentarioService arquivoService;

	@Autowired
	private ProfessorService professorService;

	private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ComentarioController.class);
	
	@Autowired
	private ComentarioService comentarioService;

	@PostMapping("/responder")
	@ResponseBody
	public ResponseEntity<?> responder(@RequestParam("texto") String texto
													,@RequestParam("comentarioPaiId") Long comentarioPaiId
													,@RequestParam(value = "files", required=false) MultipartFile[] files
													,HttpServletRequest request) {

		// Esse mapping só cria o comentário filho de outro comentário. Não há relação direta dele com a avaliação
		
		try {
			logger.debug("Iniciando criação de comentário filho para " + comentarioPaiId);
			
			String usuarioEmail = sessionService.getCurrentUser(request);
			if (usuarioEmail == null) {
				return ResponseEntity.status(401).body("Usuário não autenticado.");
			}
			
			Usuario usuario = userService.getUsuario(usuarioEmail);
			if (usuario == null) {
				return ResponseEntity.status(404).body("Usuário não encontrado.");
			}
			
			if (texto == null || texto.trim().isEmpty()) {
				return ResponseEntity.status(400).body("O texto do comentário não pode ser vazio.");
			}

			if (texto.length() > 2000) {
				return ResponseEntity.status(400).body("O texto do comentário excede o limite de 2000 caracteres.");
			}

			logger.debug("Dados validados, criando resposta.");
			// ✅ Criar resposta de comentario
			Comentario comentario = comentarioService.responderComentario(usuario, texto, comentarioPaiId);
			logger.debug("Comentário criado com ID: " + comentario.getComentarioId());

			// Cria os arquivos associados ao comentário, se houver
			if (files != null && files.length > 0) {
				boolean hasValidFiles = false;
				for (MultipartFile file : files) {
					if (!file.isEmpty()) {
						if (file.getSize() > 5 * 1024 * 1024) { // 5MB
							return ResponseEntity.status(400).body("O arquivo " + file.getOriginalFilename() + " excede o tamanho máximo de 5MB.");
						}

						try {
							arquivoService.salvarArquivo(file, comentario);
							hasValidFiles = true;
							logger.info("Arquivo " + file.getOriginalFilename() + " salvo com sucesso.");
						} catch (Exception e) {
							return ResponseEntity.status(500).body("Erro ao salvar o arquivo " + file.getOriginalFilename() + ": " + e.getMessage());
						}
					}
				}
				
				// Marcar como alarmante se tiver arquivos
				if (hasValidFiles) {
					comentario.setAlarmante(true);
					comentarioService.salvar(comentario);
				}
			}

			logger.info("Comentário salvo com sucesso.");

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "Comentário publicado com sucesso",
				"comentarioId", comentario.getComentarioId(),
				"nomeUsuario", usuario.getNome() != null ? usuario.getNome() : "Usuário"
			));

		} catch (Exception e) {
			logger.error("Erro interno: ", e);
			return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
		}
	}

	@PostMapping("/comentar")
	@ResponseBody
	public ResponseEntity<?> submitComentario(@RequestParam("texto") String texto,
														  @RequestParam("disciplinaId") String disciplinaId, 
														  @RequestParam(value = "professorId", required=true) String professorId,
														  @RequestParam(value = "files", required=false) MultipartFile[] files, 
														  HttpServletRequest request) {
		try {
			logger.info("Iniciando criação de comentário.");
			
			String usuarioEmail = sessionService.getCurrentUser(request);
			if (usuarioEmail == null) {
				return ResponseEntity.status(401).body("Usuário não autenticado.");
			}
			
			Usuario usuario = userService.getUsuario(usuarioEmail);
			if (usuario == null) {
				return ResponseEntity.status(404).body("Usuário não encontrado.");
			}
			
			Optional<Disciplina> disciplinaOpt = disciplinaService.buscarPorCodigo(disciplinaId);
			if (!disciplinaOpt.isPresent()) {
				return ResponseEntity.status(404).body("Disciplina não encontrada.");
			}
			Disciplina disciplina = disciplinaOpt.get();

			// Professor é obrigatório para comentários
			if (professorId == null || professorId.isEmpty() || professorId.equals("null")) {
				return ResponseEntity.status(400).body("É necessário selecionar um professor para comentar.");
			}
			
			Optional<Professor> professorOpt = professorService.buscarPorId(professorId);
			if (!professorOpt.isPresent()) {
				return ResponseEntity.status(404).body("Professor não encontrado.");
			}
			Professor professor = professorOpt.get();

			if (texto == null || texto.trim().isEmpty()) {
				return ResponseEntity.status(400).body("O texto do comentário não pode ser vazio.");
			}

			if (texto.length() > 2000) {
				return ResponseEntity.status(400).body("O texto do comentário excede o limite de 2000 caracteres.");
			}

			logger.info("Dados validados, criando comentário.");
			// ✅ Criar comentário com disciplina e professor (professor agora é obrigatório)
			Comentario comentario = comentarioService.criarComentario(usuario, texto, disciplina, professor);
			logger.info("Comentário criado com ID: " + comentario.getComentarioId());

			// Cria os arquivos associados ao comentário, se houver
			if (files != null && files.length > 0) {
				boolean hasValidFiles = false;
				for (MultipartFile file : files) {
					if (!file.isEmpty()) {
						if (file.getSize() > 5 * 1024 * 1024) { // 5MB
							return ResponseEntity.status(400).body("O arquivo " + file.getOriginalFilename() + " excede o tamanho máximo de 5MB.");
						}

						try {
							arquivoService.salvarArquivo(file, comentario);
							hasValidFiles = true;
							logger.info("Arquivo " + file.getOriginalFilename() + " salvo com sucesso.");
						} catch (Exception e) {
							return ResponseEntity.status(500).body("Erro ao salvar o arquivo " + file.getOriginalFilename() + ": " + e.getMessage());
						}
					}
				}
				
				// Marcar como alarmante se tiver arquivos
				if (hasValidFiles) {
					comentario.setAlarmante(true);
					comentarioService.salvar(comentario);
				}
			}

			logger.info("Comentário salvo com sucesso.");

			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "Comentário publicado com sucesso",
				"comentarioId", comentario.getComentarioId(),
				"nomeUsuario", usuario.getNome() != null ? usuario.getNome() : "Usuário"
			));

		} catch (Exception e) {
			logger.error("Erro interno: ", e);
			return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
		}
	}
	
	@PostMapping("/votar/{id}")
	@ResponseBody
	public ResponseEntity<?> vote(@PathVariable("id") Long comentarioId, @RequestParam("isUpVote") Boolean isUpVote, HttpServletRequest request){
		String userEmail = sessionService.getCurrentUser(request);
		
		if (userEmail == null) {
			return ResponseEntity.status(401).body("Usuário não autenticado.");
		}
		
		logger.debug("Registrando voto para comentário ID " + comentarioId + " por usuário " + userEmail + " como " + (isUpVote ? "upvote" : "downvote"));
		try {
			comentarioService.vote(userEmail, comentarioId, isUpVote);
			
			// Buscar comentário atualizado para retornar contadores
			Optional<com.example.model.Comentario> comentarioOpt = comentarioService.buscarPorId(comentarioId);
			if (comentarioOpt.isEmpty()) {
				return ResponseEntity.status(404).body("Comentário não encontrado.");
			}
			
			com.example.model.Comentario comentario = comentarioOpt.get();
			
			// ✅ Buscar o voto atual do usuário
			Boolean userVote = comentario.getVotes().get(userEmail); // true (upvote), false (downvote), null (no vote)
			
			return ResponseEntity.ok(Map.of(
				"success", true,
				"upVotes", comentario.getUpVotes(),
				"downVotes", comentario.getDownVotes(),
				"userVote", userVote != null ? (userVote ? 1 : -1) : 0, // Enviar 0 se for null
				"message", "Voto registrado com sucesso."
			));
		} catch(IllegalArgumentException e) {
			return ResponseEntity.status(404).body(e.getMessage());
		} catch(Exception e) {
			return ResponseEntity.status(500).body("Erro ao registrar voto: " + e.getMessage());
		}
	}

	@PostMapping("/delete/{id}")
	@ResponseBody
	@Transactional // ✅ Needed to avoid lazy initialization exception when deleting comments
	public ResponseEntity<?> delete(@PathVariable("id") Long comentarioId, HttpServletRequest request){
		String userEmail = sessionService.getCurrentUser(request);
		
		if (userEmail == null) {
			return ResponseEntity.status(401).body("Usuário não autenticado.");
		}
		logger.debug("Email do usuário para deleção: " + userEmail);

		Usuario user = userService.getUsuario(userEmail);
		if (user == null) {
			return ResponseEntity.status(401).body("Usuário não encontrado.");
		}
		logger.debug("Usuário encontrado");
		
		Comentario comentario = comentarioService.buscarPorId(comentarioId).orElse(null);
		if (comentario == null) {
			return ResponseEntity.status(404).body("Comentário não encontrado.");
		}
		logger.debug("Comentário encontrado: " + comentarioId);

		if(user.getIsAdmin() || comentario.getUsuario().getEmail().equals(userEmail)) {
			logger.debug("Usuário autorizado a deletar o comentário ID " + comentarioId);
			try {
				comentarioService.delete(comentario);
				return ResponseEntity.ok("Comentário deletado com sucesso.");
			} catch(Exception e) {
				return ResponseEntity.status(500).body("Erro ao deletar comentário: " + e.getMessage());
			}
		} else {
				return ResponseEntity.status(403).body("Permissão negada para deletar este comentário.");
		}
	}

	@PostMapping("/editar/{id}")
	@ResponseBody
	@Transactional // ✅ Needed to avoid lazy initialization exception when saving files
	public ResponseEntity<?> edit(
			@PathVariable("id") Long comentarioId, 
			@RequestParam(value = "novoTexto", required=true) String novoTexto, 
			@RequestParam(value = "files", required=false) MultipartFile[] files,
			@RequestParam(value = "existingFileIds", required=false) List<Long> existingFileIds,
			HttpServletRequest request) {
		String userEmail = sessionService.getCurrentUser(request);
		
		logger.debug("Iniciando edição do comentário ID " + comentarioId);

		if (userEmail == null) {
			return ResponseEntity.status(401).body("Usuário não autenticado.");
		}

		Usuario user = userService.getUsuario(userEmail);
		if (user == null) {
			return ResponseEntity.status(404).body("Usuário não encontrado.");
		}
		
		Comentario comentario = comentarioService.buscarPorId(comentarioId).orElse(null);
		if (comentario == null) {
			return ResponseEntity.status(404).body("Comentário não encontrado.");
		}

		if (novoTexto == null || novoTexto.trim().isEmpty()) {
			return ResponseEntity.status(400).body("O texto do comentário não pode ser vazio.");
		}
		if (novoTexto.length() > 2000) {
			return ResponseEntity.status(400).body("O texto do comentário excede o limite de 2000 caracteres.");
		}
		if(!comentario.getUsuario().getEmail().equals(userEmail)) {
			return ResponseEntity.status(403).body("Permissão negada para editar este comentário.");
		}

		List<ArquivoComentario> arquivosExistentes = arquivoService.buscarPorComentarioId(comentarioId);
		
		// Delete only files that are NOT in the existingFileIds list
		logger.debug("Verificando quais arquivos manter/deletar para o comentário ID " + comentarioId);
		for (ArquivoComentario arquivo : arquivosExistentes) {
			boolean shouldKeep = existingFileIds != null && existingFileIds.contains(arquivo.getId());
			if (!shouldKeep) {
				try {
					logger.debug("Deletando arquivo ID " + arquivo.getId() + " (" + arquivo.getNomeOriginal() + ")");
					arquivoService.deletar(arquivo.getId());
				} catch(Exception e) {
					return ResponseEntity.status(500).body("Erro ao deletar arquivo: " + e.getMessage());
				}
			} else {
				logger.debug("Mantendo arquivo ID " + arquivo.getId() + " (" + arquivo.getNomeOriginal() + ")");
			}
		}

		try {
			comentarioService.edit(comentario, novoTexto);
			logger.debug("Comentário ID " + comentarioId + " editado com sucesso.");
			
			// Save new files
			if (files != null && files.length > 0) {
				logger.debug("Salvando " + files.length + " novos arquivos para o comentário ID " + comentarioId);
				// ✅ Refresh comentario to avoid lazy initialization issues
				comentario = comentarioService.buscarPorId(comentarioId).orElse(null);
				if (comentario == null) {
					return ResponseEntity.status(404).body("Comentário não encontrado após edição.");
				}
				
				for (MultipartFile file : files) {
					if (!file.isEmpty()) {
						if (file.getSize() > 5 * 1024 * 1024) { // 5MB
							return ResponseEntity.status(400).body("O arquivo " + file.getOriginalFilename() + " excede o tamanho máximo de 5MB.");
						}
						try {
							arquivoService.salvarArquivo(file, comentario);
							logger.info("Arquivo " + file.getOriginalFilename() + " salvo com sucesso.");
						} catch (Exception e) {
							return ResponseEntity.status(500).body("Erro ao salvar o arquivo " + file.getOriginalFilename() + ": " + e.getMessage());
						}
					}
				}
			}
			logger.debug("Edição do comentário ID " + comentarioId + " concluída com sucesso.");
			// ✅ Return JSON response instead of plain text
			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "Comentário editado com sucesso"
			));
		
		} catch(Exception e) {
			return ResponseEntity.status(500).body("Erro ao editar comentário: " + e.getMessage());
		}
	}

	/**
	 * Denunciar um comentário
	 */
	@PostMapping("/denunciar/{id}")
	@ResponseBody
	public ResponseEntity<?> denunciar(@PathVariable("id") Long comentarioId, HttpServletRequest request) {
		String userEmail = sessionService.getCurrentUser(request);
		
		if (userEmail == null) {
			return ResponseEntity.status(401).body("Usuário não autenticado.");
		}
		
		logger.debug("Denunciando comentário ID " + comentarioId + " por usuário " + userEmail);
		
		try {
			boolean denunciaAdicionada = comentarioService.denunciar(comentarioId, userEmail);
			
			if (!denunciaAdicionada) {
				return ResponseEntity.status(409).body(Map.of(
					"success", false,
					"message", "Você já denunciou este comentário anteriormente."
				));
			}
			
			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "Comentário denunciado com sucesso. Um administrador irá revisar."
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(404).body("Comentário não encontrado.");
		} catch (Exception e) {
			logger.error("Erro ao denunciar comentário: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao denunciar comentário: " + e.getMessage());
		}
	}
	
}