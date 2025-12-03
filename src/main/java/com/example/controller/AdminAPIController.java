package com.example.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.Comentario;
import com.example.model.Usuario;
import com.example.scrapper.DisciplinaScrapper;
import com.example.service.ComentarioService;
import com.example.service.ScrapperStatusService;
import com.example.service.SessionService;
import com.example.service.UsuarioService;

import com.example.DTO.AdminCommentDTO;
import com.example.DTO.UserDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador REST para fornecer os métodos para o usuário administrador.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAPIController {
	
	@Autowired
	private UsuarioService userService;
	@Autowired
	private SessionService sessionService;
	@Autowired
	private DisciplinaScrapper disciplinaScrapper;
	@Autowired
	private ScrapperStatusService scrapperStatusService;
	@Autowired
	private ComentarioService comentarioService;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdminAPIController.class);

	@PostMapping("/users")
	public ResponseEntity<ArrayList<UserDTO>> getUsers(HttpServletRequest request) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}

		List<Usuario> users = userService.getUsers();
		ArrayList<UserDTO> usersRet = new ArrayList<UserDTO>();
		users.forEach(user -> {
			usersRet.add(UserDTO.from(user));
		});
		return ResponseEntity.ok(usersRet);

	}

	@Transactional
	@PostMapping("/toggle-admin")
	public ResponseEntity<String> toggleAdmin(HttpServletRequest request, @RequestBody Map<String,String> body) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		String email = body.get("email");
		if(email != null) logger.debug("Toggle admin para: " + email);
		try {
			boolean success = userService.toggleAdmin(email);
			if(!success) {
				return ResponseEntity.status(404).body("Usuário não encontrado.");
			}
			return ResponseEntity.ok("Nível de administrador alterado com sucesso.");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Erro ao alterar nível de administrador.");
		}
	}

	@Transactional
	@PostMapping("/delete-user")
	public ResponseEntity<String> deleteUser(HttpServletRequest request, @RequestBody Map<String,String> body) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		String email = body.get("email");
		if(email != null) logger.debug("Deletar conta: " + email);
		try {
			Usuario user = userService.getUsuario(email);
			if(user == null) {
				return ResponseEntity.status(404).body("Usuário não encontrado.");
			}

			userService.delete(user);
			
			return ResponseEntity.ok("Conta deletada com sucesso.");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Erro ao deletar conta.");
		}
	}

	@Transactional
	@PostMapping("/ban-user")
	public ResponseEntity<String> banUser(HttpServletRequest request, @RequestBody Map<String,String> body) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		String email = body.get("email");
		String motivo = body.get("motivo");
		
		if(email == null || email.trim().isEmpty()) {
			return ResponseEntity.status(400).body("Email é obrigatório.");
		}
		
		logger.debug("Banir usuário: " + email);
		
		try {
			String adminEmail = sessionService.getCurrentUser(request);
			boolean success = userService.banirUsuario(email, adminEmail, motivo);
			
			if(!success) {
				return ResponseEntity.status(404).body("Usuário não encontrado ou já banido.");
			}
			
			return ResponseEntity.ok("Usuário banido com sucesso.");
		} catch (Exception e) {
			logger.error("Erro ao banir usuário: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao banir usuário.");
		}
	}

	@Transactional
	@PostMapping("/unban-user")
	public ResponseEntity<String> unbanUser(HttpServletRequest request, @RequestBody Map<String,String> body) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		String matricula = body.get("matricula");
		
		if(matricula == null || matricula.trim().isEmpty()) {
			return ResponseEntity.status(400).body("Matrícula é obrigatória.");
		}
		
		logger.debug("Desbanir matrícula: " + matricula);
		
		try {
			boolean success = userService.desbanirMatricula(matricula);
			
			if(!success) {
				return ResponseEntity.status(404).body("Matrícula não está banida.");
			}
			
			return ResponseEntity.ok("Banimento removido com sucesso.");
		} catch (Exception e) {
			logger.error("Erro ao desbanir usuário: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao remover banimento.");
		}
	}

	@PostMapping("/banned-users")
	public ResponseEntity<?> getBannedUsers(HttpServletRequest request) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			var banidos = userService.listarBanidos();
			return ResponseEntity.ok(banidos);
		} catch (Exception e) {
			logger.error("Erro ao listar usuários banidos: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao listar usuários banidos.");
		}
	}

	// ==================== COMENTÁRIOS ====================

	/**
	 * Buscar todos os comentários ordenados pelos mais recentes
	 */
	@Transactional
	@PostMapping("/comments")
	public ResponseEntity<?> getAllComments(
			HttpServletRequest request,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			Page<Comentario> comentariosPage = comentarioService.buscarTodosComentariosOrdenados(page, size);
			List<AdminCommentDTO> comentarios = comentariosPage.getContent().stream()
					.map(AdminCommentDTO::from)
					.toList();
			
			Map<String, Object> response = new HashMap<>();
			response.put("content", comentarios);
			response.put("totalElements", comentariosPage.getTotalElements());
			response.put("totalPages", comentariosPage.getTotalPages());
			response.put("currentPage", page);
			response.put("size", size);
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("Erro ao buscar comentários: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao buscar comentários.");
		}
	}

	/**
	 * Buscar comentários alarmantes (para revisão)
	 */
	@Transactional
	@PostMapping("/comments/alarming")
	public ResponseEntity<?> getAlarmingComments(
			HttpServletRequest request,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			Page<Comentario> comentariosPage = comentarioService.buscarComentariosAlarmantes(page, size);
			List<AdminCommentDTO> comentarios = comentariosPage.getContent().stream()
					.map(AdminCommentDTO::from)
					.toList();
			
			Map<String, Object> response = new HashMap<>();
			response.put("content", comentarios);
			response.put("totalElements", comentariosPage.getTotalElements());
			response.put("totalPages", comentariosPage.getTotalPages());
			response.put("currentPage", page);
			response.put("size", size);
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("Erro ao buscar comentários alarmantes: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao buscar comentários alarmantes.");
		}
	}

	/**
	 * Obter estatísticas de comentários
	 */
	@Transactional
	@PostMapping("/comments/stats")
	public ResponseEntity<?> getCommentsStats(HttpServletRequest request) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			Map<String, Object> stats = new HashMap<>();
			stats.put("totalComentarios", comentarioService.contarTodosComentarios());
			stats.put("comentariosAlarmantes", comentarioService.contarAlarmantes());
			
			return ResponseEntity.ok(stats);
		} catch (Exception e) {
			logger.error("Erro ao buscar estatísticas: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao buscar estatísticas.");
		}
	}

	/**
	 * Marcar comentário como seguro (remove da lista de alarmantes)
	 */
	@Transactional
	@PostMapping("/comments/{id}/mark-safe")
	public ResponseEntity<String> markCommentAsSafe(
			HttpServletRequest request,
			@PathVariable Long id) {
		
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			comentarioService.marcarComoSeguro(id);
			return ResponseEntity.ok("Comentário marcado como seguro.");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(404).body("Comentário não encontrado.");
		} catch (Exception e) {
			logger.error("Erro ao marcar comentário como seguro: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao marcar comentário como seguro.");
		}
	}

	/**
	 * Deletar comentário
	 */
	@Transactional
	@DeleteMapping("/comments/{id}")
	public ResponseEntity<String> deleteComment(
			HttpServletRequest request,
			@PathVariable Long id) {
		
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			comentarioService.deletar(id);
			return ResponseEntity.ok("Comentário deletado.");
		} catch (Exception e) {
			logger.error("Erro ao deletar comentário: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao deletar comentário.");
		}
	}

	/**
	 * Banir usuário a partir do ID do comentário
	 */
	@Transactional
	@PostMapping("/comments/{id}/ban-user")
	public ResponseEntity<String> banUserByComment(
			HttpServletRequest request,
			@PathVariable Long id,
			@RequestBody Map<String, String> body) {
		
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		String motivo = body.get("motivo");
		
		try {
			Usuario usuario = comentarioService.getUsuarioDoComentario(id);
			if (usuario == null) {
				return ResponseEntity.status(404).body("Usuário do comentário não encontrado.");
			}
			
			String adminEmail = sessionService.getCurrentUser(request);
			boolean success = userService.banirUsuario(usuario.getEmail(), adminEmail, motivo);
			
			if (!success) {
				return ResponseEntity.status(400).body("Não foi possível banir o usuário.");
			}
			
			return ResponseEntity.ok("Usuário banido com sucesso.");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(404).body("Comentário não encontrado.");
		} catch (Exception e) {
			logger.error("Erro ao banir usuário: " + e.getMessage());
			return ResponseEntity.status(500).body("Erro ao banir usuário.");
		}
	}

	/**
	 * Endpoint para obter status do scrapper de disciplinas
	 */
	@Transactional
	@PostMapping("/scrapper/status")
	public ResponseEntity<?> getScrapperStatus(HttpServletRequest request) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			var status = scrapperStatusService.getUltimoStatus();
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Erro ao obter status do scrapper: " + e.getMessage());
		}
	}

	/**
	 * Endpoint para executar scrapping de disciplinas
	 */
	@PostMapping("/scrapper/execute")
	public ResponseEntity<String> executeScrapper(HttpServletRequest request) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		try {
			// Obter nome do usuário atual para rastreamento
			String currentUserEmail = sessionService.getCurrentUser(request);
			String adminName = currentUserEmail != null ? currentUserEmail : "Administrador";
			
			// Executa em thread separada para não bloquear a requisição
			new Thread(() -> {
				try {
					disciplinaScrapper.executarScraping(adminName);
				} catch (Exception e) {
					System.err.println("Erro durante scraping executado por " + adminName + ": " + e.getMessage());
				}
			}).start();
			
			return ResponseEntity.ok("Scrapping iniciado com sucesso. Verifique o status para acompanhar o progresso.");
		} catch (IllegalStateException e) {
			return ResponseEntity.status(409).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Erro ao iniciar scrapping: " + e.getMessage());
		}
	}

	
}
