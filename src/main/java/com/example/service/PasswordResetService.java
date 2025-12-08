package com.example.service;

import com.example.model.PasswordResetToken;
import com.example.model.Usuario;
import com.example.repository.PasswordResetTokenRepository;
import com.example.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de recuperação de senha.
 */
@Service
public class PasswordResetService {
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private EmailService emailService;
    
	 private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * Solicita recuperação de senha para um email.
     * @return true se o email existe e o processo foi iniciado, false se não existe
     */
    @Transactional
    public boolean requestPasswordReset(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isEmpty()) {
            // Email não cadastrado - informamos o usuário
            return false;
        }
        
		  logger.info("Iniciando processo de recuperação de senha para o email: {}", email);

        Usuario usuario = usuarioOpt.get();
        
        // Invalida tokens anteriores do usuário
        tokenRepository.invalidateAllUserTokens(usuario);
        
        // Cria novo token
        PasswordResetToken token = new PasswordResetToken(usuario);
        tokenRepository.save(token);
        
        // Envia email (assíncrono)
        emailService.sendPasswordResetEmail(email, token.getToken());
        
        return true;
    }
    
    /**
     * Valida se um token é válido.
     */
    public boolean isTokenValid(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        return tokenOpt.map(PasswordResetToken::isValid).orElse(false);
    }
    
    /**
     * Obtém o email associado a um token válido.
     */
	 @Transactional(readOnly = true)
    public Optional<String> getEmailByToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(PasswordResetToken::isValid)
                .map(t -> t.getUsuario().getEmail());
    }
    
    /**
     * Redefine a senha usando um token válido.
     * @return true se a senha foi alterada com sucesso
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        if (!resetToken.isValid()) {
            return false;
        }
        
        // Atualiza a senha do usuário
        Usuario usuario = resetToken.getUsuario();
        String hashedPassword = HashingService.hashPassword(newPassword);
        usuario.setPassword(hashedPassword);
        usuarioRepository.save(usuario);
        
        // Marca o token como usado
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        // Invalida outros tokens do usuário
        tokenRepository.invalidateAllUserTokens(usuario);
        
        // Envia email de confirmação de alteração de senha
        emailService.sendPasswordChangedEmail(usuario.getEmail());
        
        return true;
    }
    
    /**
     * Limpa tokens expirados e usados do banco.
     * Pode ser chamado por um job agendado.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredAndUsedTokens(Instant.now());
    }
    
    /**
     * Invalida todos os tokens de recuperação de um usuário pelo email.
     * Usado quando o usuário cancela o processo de recuperação.
     */
    @Transactional
    public void invalidateTokensByEmail(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        usuarioOpt.ifPresent(usuario -> {
            tokenRepository.invalidateAllUserTokens(usuario);
            logger.info("Tokens de recuperação invalidados para o email: {}", email);
        });
    }
}
