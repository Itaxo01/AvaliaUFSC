-- Tabela para rastrear quais usuários denunciaram cada comentário
-- Isso impede que um mesmo usuário denuncie múltiplas vezes o mesmo comentário

CREATE TABLE IF NOT EXISTS comentario_denuncias (
    comentario_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    PRIMARY KEY (comentario_id, user_email),
    CONSTRAINT fk_denuncia_comentario FOREIGN KEY (comentario_id) 
        REFERENCES comentarios(comentario_id) ON DELETE CASCADE
);

-- Índice para buscar denúncias por comentário
CREATE INDEX IF NOT EXISTS idx_denuncias_comentario ON comentario_denuncias(comentario_id);
