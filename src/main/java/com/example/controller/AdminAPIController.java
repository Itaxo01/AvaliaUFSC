package com.example.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.Usuario;
import com.example.scrapper.DisciplinaScrapper;
import com.example.service.ScrapperStatusService;
import com.example.service.SessionService;
import com.example.service.UsuarioService;

import com.example.DTO.UserDTO;

import jakarta.servlet.http.HttpServletRequest;

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

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdminAPIController.class);

	@GetMapping("/users")
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

	@GetMapping("/banned-users")
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

	/**
	 * Endpoint para obter status do scrapper de disciplinas
	 */
	@GetMapping("/scrapper/status")
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
	public ResponseEntity<String> executeScrapper(HttpServletRequest request, @RequestBody Map<String,String> body) {
		boolean auth = sessionService.verifySession(request);
		if (!auth || !sessionService.currentUserIsAdmin(request)) {
			return ResponseEntity.status(403).build();
		}
		
		String cagrUsername = body.get("cagrUsername");
		String cagrPassword = body.get("cagrPassword");
		
		if (cagrUsername == null || cagrUsername.trim().isEmpty() || 
			cagrPassword == null || cagrPassword.trim().isEmpty()) {
			return ResponseEntity.status(400).body("Usuário e senha do CAGR são obrigatórios.");
		}
		
		try {
			// Obter nome do usuário atual para rastreamento
			String currentUserEmail = sessionService.getCurrentUser(request);
			String adminName = currentUserEmail != null ? currentUserEmail : "Administrador";
			
			// Executa em thread separada para não bloquear a requisição
			new Thread(() -> {
				try {
					disciplinaScrapper.executarScraping(cagrUsername.trim(), cagrPassword.trim(), adminName);
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
