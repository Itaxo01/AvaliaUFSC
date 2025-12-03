-- Script de migração para atualizar dados de comentários existentes
-- As colunas alarmante, denunciado e denuncias_count já foram criadas na V1

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
