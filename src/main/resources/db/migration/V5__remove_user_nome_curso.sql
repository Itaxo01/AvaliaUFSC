-- Remoção de colunas de nome e curso para garantir anonimato dos usuários

-- Remover coluna 'nome' da tabela usuarios
ALTER TABLE usuarios DROP COLUMN IF EXISTS nome;

-- Remover coluna 'curso' da tabela usuarios
ALTER TABLE usuarios DROP COLUMN IF EXISTS curso;

-- Remover coluna 'nome' da tabela usuarios_banidos
ALTER TABLE usuarios_banidos DROP COLUMN IF EXISTS nome;
