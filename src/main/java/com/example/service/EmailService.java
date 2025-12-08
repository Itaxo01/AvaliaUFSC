package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Serviço responsável pelo envio de emails.
 */
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    /**
     * Envia email de recuperação de senha de forma assíncrona.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("AvaliaUFSC - Recuperação de Senha");
            
            String resetLink = baseUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailHtml(resetLink);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Email de recuperação enviado para: " + toEmail);
            
        } catch (MessagingException e) {
            System.err.println("Erro ao enviar email de recuperação: " + e.getMessage());
            throw new RuntimeException("Falha ao enviar email de recuperação", e);
        }
    }
    
    private String buildPasswordResetEmailHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 8px; margin-top: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #2c3e50; margin: 0; font-size: 28px;">AvaliaUFSC</h1>
                        <p style="color: #7f8c8d; margin-top: 5px;">Recuperação de Senha</p>
                    </div>
                    
                    <div style="color: #34495e; line-height: 1.6;">
                        <p>Olá,</p>
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta no AvaliaUFSC.</p>
                        <p>Clique no botão abaixo para criar uma nova senha:</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="display: inline-block; background-color: #3498db; color: #ffffff; padding: 14px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;">
                                Redefinir Senha
                            </a>
                        </div>
                        
                        <p style="color: #7f8c8d; font-size: 14px;">
                            Se você não solicitou esta alteração, ignore este email. Sua senha permanecerá a mesma.
                        </p>
                        
                        <p style="color: #7f8c8d; font-size: 14px;">
                            Este link expira em <strong>3 horas</strong>.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #ecf0f1; margin: 30px 0;">
                        
                        <p style="color: #95a5a6; font-size: 12px; text-align: center;">
                            Se o botão não funcionar, copie e cole o link abaixo no seu navegador:<br>
                            <a href="%s" style="color: #3498db; word-break: break-all;">%s</a>
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ecf0f1;">
                        <p style="color: #95a5a6; font-size: 12px; margin: 0;">
                            © 2024 AvaliaUFSC - Sistema de Avaliação de Professores
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, resetLink);
    }
    
    /**
     * Envia email de confirmação de alteração de senha.
     */
    @Async
    public void sendPasswordChangedEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("AvaliaUFSC - Sua senha foi alterada");
            
            String htmlContent = buildPasswordChangedEmailHtml();
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Email de confirmação de alteração enviado para: " + toEmail);
            
        } catch (MessagingException e) {
            System.err.println("Erro ao enviar email de confirmação: " + e.getMessage());
            // Não lançamos exceção pois é apenas notificação
        }
    }
    
    private String buildPasswordChangedEmailHtml() {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 8px; margin-top: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #2c3e50; margin: 0; font-size: 28px;">AvaliaUFSC</h1>
                        <p style="color: #7f8c8d; margin-top: 5px;">Confirmação de Alteração</p>
                    </div>
                    
                    <div style="color: #34495e; line-height: 1.6;">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <span style="font-size: 48px;">✅</span>
                        </div>
                        
                        <h2 style="text-align: center; color: #27ae60;">Senha alterada com sucesso!</h2>
                        
                        <p>Olá,</p>
                        <p>Sua senha do AvaliaUFSC foi alterada com sucesso.</p>
                        
                        <div style="background-color: #fef9e7; border-left: 4px solid #f39c12; padding: 15px; margin: 20px 0;">
                            <p style="margin: 0; color: #856404;">
                                <strong>⚠️ Não foi você?</strong><br>
                                Se você não realizou esta alteração, sua conta pode estar comprometida. 
                                Entre em contato conosco imediatamente.
                            </p>
                        </div>
                        
                        <p style="color: #7f8c8d; font-size: 14px;">
                            Esta é uma mensagem automática de segurança.
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ecf0f1;">
                        <p style="color: #95a5a6; font-size: 12px; margin: 0;">
                            © 2024 AvaliaUFSC - Sistema de Avaliação de Professores
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
