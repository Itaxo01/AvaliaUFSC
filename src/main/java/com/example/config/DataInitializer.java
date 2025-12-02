package com.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.service.UsuarioService;
import com.example.service.HashingService;

/**
 * Inicializa o banco de dados com dados padrão na primeira execução.
 * Cria um usuário administrador padrão se não existir.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioService userService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.matricula}")
    private String adminMatricula;

	 private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataInitializer.class);

    @Override
    public void run(String... args) throws Exception {
        // Verifica se o usuário administrador específico já existe
        if (userService.getUsuario(adminEmail) == null) {
            logger.info("Criando usuário administrador...");
            
            userService.create(adminEmail, adminPassword, adminMatricula);
            
            logger.info("Usuário administrador criado com sucesso!");

			} else {
				logger.info("Usuário administrador já existe: " + adminEmail);
			}
			if(!userService.getIsAdmin(adminEmail)) {
				logger.info(" Definindo usuário como administrador...");
				userService.toggleAdmin(adminEmail);
			} else {
				logger.info(" Usuário já possui privilégios de administrador: " + adminEmail);
			}
    }
}
