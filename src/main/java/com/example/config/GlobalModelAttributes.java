package com.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.model.Usuario;
import com.example.service.SessionService;
import com.example.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adiciona atributos globais ao modelo de todas as views.
 * Evita repetição de código nos controllers.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UsuarioService userService;

    /**
     * Versão dos assets estáticos (CSS, JS) para cache busting.
     * Altere este valor em application-*.properties para forçar atualização do cache.
     * Configuração: app.assets.version=2.0.1
     */
    @Value("${app.assets.version:2.0.0}")
    private String assetsVersion;

    /**
     * Disponibiliza a versão dos assets em todas as views como ${v}
     */
    @ModelAttribute("v")
    public String getAssetsVersion() {
        return assetsVersion;
    }


    /**
     * Adiciona informações do usuário logado em todas as páginas
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request) {
        String userEmail = sessionService.getCurrentUser(request);
        
        if (userEmail != null) {
            Usuario usuario = userService.getUsuario(userEmail);
            
            if (usuario != null) {
                // Adicionar informações do usuário
                model.addAttribute("userEmail", userEmail);
                model.addAttribute("isAdmin", usuario.getIsAdmin());
                
                // Adicionar inicial do usuário para o avatar (baseado no email)
                String email = usuario.getEmail();
                if (email != null && email.length() >= 2) {
                    model.addAttribute("userInitial", email.substring(0, 2).toUpperCase());
                } else if (email != null && email.length() == 1) {
                    model.addAttribute("userInitial", email.substring(0, 1).toUpperCase());
                } else {
                    model.addAttribute("userInitial", "U");
                }
            }
        }
    }
}