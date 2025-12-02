-- Script de migração para adicionar colunas de revisão de comentários
-- Este script é executado automaticamente se você usar Flyway,
-- ou pode ser executado manualmente no banco de dados

-- Adicionar colunas (caso não existam - para PostgreSQL)
ALTER TABLE comentarios ADD COLUMN IF NOT EXISTS alarmante BOOLEAN DEFAULT FALSE;
ALTER TABLE comentarios ADD COLUMN IF NOT EXISTS denunciado BOOLEAN DEFAULT FALSE;
ALTER TABLE comentarios ADD COLUMN IF NOT EXISTS denuncias_count INTEGER DEFAULT 0;

-- Para H2 (desenvolvimento), as colunas são criadas automaticamente pelo Hibernate
-- Este script só precisa atualizar os dados existentes

-- Atualizar comentários com texto longo (> 150 caracteres) como alarmantes
UPDATE comentarios 
SET alarmante = TRUE 
WHERE LENGTH(texto) > 150 AND (alarmante IS NULL OR alarmante = FALSE);

-- Atualizar comentários que têm arquivos como alarmantes
UPDATE comentarios c
SET alarmante = TRUE
WHERE EXISTS (
    SELECT 1 FROM arquivos_comentario a 
    WHERE a.comentario_id = c.comentario_id
) AND (c.alarmante IS NULL OR c.alarmante = FALSE);

-- Garantir que valores NULL sejam convertidos para FALSE
UPDATE comentarios SET alarmante = FALSE WHERE alarmante IS NULL;
UPDATE comentarios SET denunciado = FALSE WHERE denunciado IS NULL;
UPDATE comentarios SET denuncias_count = 0 WHERE denuncias_count IS NULL;
