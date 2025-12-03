package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.service.PdfValidationService;
import com.example.service.UsuarioService;

import org.springframework.http.MediaType;

/**
 * Controlador responsável pelo fluxo de registro de novos usuários.
 */
@Controller
public class RegisterController {

	@Autowired
    private UsuarioService userService;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RegisterController.class);
	
	/**
	 * Recebe o formulário de registro, valida o PDF e cria o usuário no repositório.
	 *
	 * @param email    email do usuário
	 * @param password senha em texto claro enviada pelo formulário
	 * @param pdf      comprovante em PDF para validação
	 * @param model    modelo para exibir mensagens de erro ao usuário
	 * @param redirectAttributes atributos para passar mensagens após redirect
	 * @return redirect para "/login" em caso de sucesso ou view "register" com erros
	 */


	@PostMapping(value = "/register/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String handleRegister(@RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("pdf") MultipartFile pdf, Model model, RedirectAttributes redirectAttributes) {
		logger.debug("Tentativa de registro: " + email);
		logger.debug("PDF recebido: " + (pdf != null ? pdf.getOriginalFilename() : "null"));
		
		PdfValidationService.ValidationResult valid = PdfValidationService.validate(pdf);
		if (!valid.valid()) {
			logger.debug(valid.message());
			model.addAttribute("error", valid.message());
			model.addAttribute("emailValue", email);
			model.addAttribute("passwordValue", password);
			return "register";
		}

		// Verifica se a matrícula está banida
		if (userService.isMatriculaBanida(valid.matricula())) {
			logger.warn("Tentativa de registro com matrícula banida: " + valid.matricula());
			model.addAttribute("error", "Esta matrícula foi banida do sistema. Entre em contato com a administração.");
			model.addAttribute("emailValue", email);
			model.addAttribute("passwordValue", password);
			return "register";
		}

		try {
			userService.create(email, password, valid.matricula());

			redirectAttributes.addFlashAttribute("successMessage", "Registro concluído, por favor faça o login");
			return "redirect:/login";
		} catch (Exception e) {
			logger.error("Erro ao criar usuário: " + e.getMessage());

			model.addAttribute("error", "Erro ao criar usuário: " + e.getMessage());
			model.addAttribute("emailValue", email);
			model.addAttribute("passwordValue", password);

			return "register";
		}
    }
}
