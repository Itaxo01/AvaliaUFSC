package com.example.controller;

import com.example.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;

/**
 * Controller responsável pelas páginas de recuperação de senha.
 */
@Controller
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PasswordResetController.class);
    
    // Cooldown de 5 minutos (300 segundos)
    private static final int COOLDOWN_SECONDS = 300;
    
    // Chaves de sessão
    private static final String SESSION_LAST_RESET_REQUEST = "lastResetRequest";
    private static final String SESSION_RESET_EMAIL = "resetEmail";

    /**
     * Exibe a página de "Esqueci minha senha"
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(HttpSession session, Model model) {
        // Se já tem um email em processo de recuperação, redireciona para tela de espera
        String resetEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (resetEmail != null) {
            return "redirect:/forgot-password/waiting";
        }
        return "forgot-password";
    }
    
    /**
     * Processa a solicitação de recuperação de senha
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, 
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        // Verifica cooldown
        Instant lastRequest = (Instant) session.getAttribute(SESSION_LAST_RESET_REQUEST);
        if (lastRequest != null) {
            long secondsSinceLastRequest = Instant.now().getEpochSecond() - lastRequest.getEpochSecond();
            if (secondsSinceLastRequest < COOLDOWN_SECONDS) {
                long remainingSeconds = COOLDOWN_SECONDS - secondsSinceLastRequest;
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                redirectAttributes.addFlashAttribute("error", 
                    String.format("Aguarde %d:%02d antes de solicitar outro email.", minutes, seconds));
                return "redirect:/forgot-password";
            }
        }
        
        boolean emailExists = passwordResetService.requestPasswordReset(email);
        
        if (emailExists) {
            // Salva na sessão o email e o timestamp
            session.setAttribute(SESSION_RESET_EMAIL, email);
            session.setAttribute(SESSION_LAST_RESET_REQUEST, Instant.now());
            
            // Redireciona para tela de espera
            return "redirect:/forgot-password/waiting";
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Não existe uma conta cadastrada com este email.");
            return "redirect:/forgot-password";
        }
    }
    
    /**
     * Exibe a tela de espera após solicitar recuperação
     */
    @GetMapping("/forgot-password/waiting")
    public String showWaitingPage(HttpSession session, Model model) {
        String resetEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);
        
        if (resetEmail == null) {
            return "redirect:/forgot-password";
        }
        
        model.addAttribute("email", resetEmail);
        
        // Calcula tempo restante do cooldown
        Instant lastRequest = (Instant) session.getAttribute(SESSION_LAST_RESET_REQUEST);
        if (lastRequest != null) {
            long secondsSinceLastRequest = Instant.now().getEpochSecond() - lastRequest.getEpochSecond();
            long remainingSeconds = Math.max(0, COOLDOWN_SECONDS - secondsSinceLastRequest);
            model.addAttribute("cooldownSeconds", remainingSeconds);
            model.addAttribute("canResend", remainingSeconds == 0);
        } else {
            model.addAttribute("cooldownSeconds", 0);
            model.addAttribute("canResend", true);
        }
        
        return "forgot-password-waiting";
    }
    
    /**
     * Reenvia o email de recuperação
     */
    @PostMapping("/forgot-password/resend")
    public String resendResetEmail(HttpSession session, RedirectAttributes redirectAttributes) {
        String resetEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);
        
        if (resetEmail == null) {
            return "redirect:/forgot-password";
        }
        
        // Verifica cooldown
        Instant lastRequest = (Instant) session.getAttribute(SESSION_LAST_RESET_REQUEST);
        if (lastRequest != null) {
            long secondsSinceLastRequest = Instant.now().getEpochSecond() - lastRequest.getEpochSecond();
            if (secondsSinceLastRequest < COOLDOWN_SECONDS) {
                long remainingSeconds = COOLDOWN_SECONDS - secondsSinceLastRequest;
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                redirectAttributes.addFlashAttribute("error", 
                    String.format("Aguarde %d:%02d antes de solicitar outro email.", minutes, seconds));
                return "redirect:/forgot-password/waiting";
            }
        }
        
        // Reenvia o email
        boolean success = passwordResetService.requestPasswordReset(resetEmail);
        
        if (success) {
            session.setAttribute(SESSION_LAST_RESET_REQUEST, Instant.now());
            redirectAttributes.addFlashAttribute("successMessage", 
                "Um novo link foi enviado para " + resetEmail);
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Erro ao reenviar o email. Tente novamente.");
        }
        
        return "redirect:/forgot-password/waiting";
    }
    
    /**
     * Cancela o processo de recuperação e volta para o início
     */
    @GetMapping("/forgot-password/cancel")
    public String cancelReset(HttpSession session) {
        String resetEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);
        
        // Invalida os tokens do usuário
        if (resetEmail != null) {
            passwordResetService.invalidateTokensByEmail(resetEmail);
        }
        
        session.removeAttribute(SESSION_RESET_EMAIL);
        session.removeAttribute(SESSION_LAST_RESET_REQUEST);
        return "redirect:/forgot-password";
    }
    
    /**
     * Exibe a página de redefinição de senha (acessada pelo link do email)
     */
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
		  logger.debug("Acessando página de redefinição de senha com token: {}", token);

        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("error", "Link inválido ou expirado. Solicite um novo link de recuperação.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }
        
        model.addAttribute("token", token);
        passwordResetService.getEmailByToken(token).ifPresent(email -> 
            model.addAttribute("email", email)
        );
        
        return "reset-password";
    }
    
    /**
     * Processa a redefinição de senha
     */
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        
        // Valida se as senhas coincidem
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "As senhas não coincidem.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        
        // Valida força da senha
        if (password.length() < 8) {
            model.addAttribute("error", "A senha deve ter no mínimo 8 caracteres.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        
        // Tenta redefinir a senha
        boolean success = passwordResetService.resetPassword(token, password);
        
        if (!success) {
            model.addAttribute("error", "Link inválido ou expirado. Solicite um novo link de recuperação.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }
        
        // Limpa dados da sessão de recuperação
        session.removeAttribute(SESSION_RESET_EMAIL);
        session.removeAttribute(SESSION_LAST_RESET_REQUEST);
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "Senha alterada com sucesso! Faça login com sua nova senha.");
        
        return "redirect:/login";
    }
}
