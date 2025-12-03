package com.example.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.example.model.Usuario;
import com.example.model.Disciplina;
import com.example.service.SessionService;
import com.example.service.UsuarioService;
import com.example.service.DisciplinaService;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class IndexController {
	
	@Autowired
	private SessionService sessionService;

	@Autowired
	private UsuarioService userService;

	@Autowired
	private DisciplinaService disciplinaService;

	@GetMapping("/index")
	public String index(Model model, HttpServletRequest request) {
		String userEmail = sessionService.getCurrentUser(request);
		
		if (userEmail == null) {
			return "redirect:/login";
		}
		
		Usuario usuario = userService.getUsuario(userEmail);

		if(usuario == null) { // possivel erro fatal
			return "redirect:/login";
		}
		
		return "index";
	}
}
