package com.example.repository;

import com.example.model.PasswordResetToken;
import com.example.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    List<PasswordResetToken> findByUsuarioAndUsedFalse(Usuario usuario);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now OR t.used = true")
    void deleteExpiredAndUsedTokens(Instant now);
    
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.usuario = :usuario AND t.used = false")
    void invalidateAllUserTokens(Usuario usuario);
}
