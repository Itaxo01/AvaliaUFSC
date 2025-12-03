-- Criação da tabela de usuários banidos (se não existir)
CREATE TABLE IF NOT EXISTS usuarios_banidos (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    banido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    banido_por VARCHAR(255),
    motivo VARCHAR(500)
);

-- Remoção de colunas de nome e curso para garantir anonimato dos usuários

-- Remover coluna 'nome' da tabela usuarios
ALTER TABLE usuarios DROP COLUMN IF EXISTS nome;

-- Remover coluna 'curso' da tabela usuarios
ALTER TABLE usuarios DROP COLUMN IF EXISTS curso;
