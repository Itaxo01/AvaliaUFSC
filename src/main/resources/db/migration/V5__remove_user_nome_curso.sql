-- A tabela usuarios_banidos já foi criada na V1
-- Este script apenas remove colunas antigas se existirem

-- Remover coluna 'nome' da tabela usuarios (se existir)
ALTER TABLE usuarios DROP COLUMN IF EXISTS nome;

-- Remover coluna 'curso' da tabela usuarios (se existir)
ALTER TABLE usuarios DROP COLUMN IF EXISTS curso;
