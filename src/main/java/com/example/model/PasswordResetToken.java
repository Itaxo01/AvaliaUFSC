package com.example.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade que representa um token de recuperação de senha.
 * <p>O token é gerado quando o usuário solicita a recuperação de senha
 * e é invalidado após uso ou expiração (3 horas).</p>
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    
    private static final int EXPIRATION_HOURS = 3;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
    
    @Column(name = "used", nullable = false)
    private boolean used = false;
    
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    
    public PasswordResetToken() {}
    
    public PasswordResetToken(Usuario usuario) {
        this.usuario = usuario;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = Instant.now().plusSeconds(EXPIRATION_HOURS * 3600);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
