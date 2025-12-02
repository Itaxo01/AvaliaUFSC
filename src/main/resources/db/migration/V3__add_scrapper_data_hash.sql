-- Adiciona coluna data_hash na tabela scrapper_status
-- Esta coluna armazena um hash SHA-256 dos dados do último scrapper bem-sucedido
-- O hash é usado pelo frontend para verificar se o cache precisa ser atualizado

ALTER TABLE scrapper_status ADD COLUMN IF NOT EXISTS data_hash VARCHAR(64);

-- Comentário da coluna (para PostgreSQL)
COMMENT ON COLUMN scrapper_status.data_hash IS 'Hash SHA-256 dos dados do último scrapper bem-sucedido (disciplinas_capturadas + professores_capturados + ultimo_sucesso)';
