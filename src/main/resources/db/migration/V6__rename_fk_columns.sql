-- =============================================
-- V6: Renomear colunas FK para nomes corretos
-- =============================================
-- As colunas foram geradas automaticamente pelo Hibernate com nomes como:
-- - disciplina_disciplina_id -> disciplina_id
-- - professor_professor_id -> professor_id
-- Esta migração corrige os nomes para corresponder aos @JoinColumn explícitos

-- =============================================
-- TABELA: avaliacoes
-- =============================================

-- Renomear disciplina_disciplina_id para disciplina_id (se existir com nome antigo)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'avaliacoes' AND column_name = 'disciplina_disciplina_id'
    ) THEN
        ALTER TABLE avaliacoes RENAME COLUMN disciplina_disciplina_id TO disciplina_id;
    END IF;
END $$;

-- Renomear professor_professor_id para professor_id (se existir com nome antigo)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'avaliacoes' AND column_name = 'professor_professor_id'
    ) THEN
        ALTER TABLE avaliacoes RENAME COLUMN professor_professor_id TO professor_id;
    END IF;
END $$;

-- =============================================
-- TABELA: comentarios
-- =============================================

-- Renomear disciplina_disciplina_id para disciplina_id (se existir com nome antigo)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'comentarios' AND column_name = 'disciplina_disciplina_id'
    ) THEN
        ALTER TABLE comentarios RENAME COLUMN disciplina_disciplina_id TO disciplina_id;
    END IF;
END $$;

-- Renomear professor_professor_id para professor_id (se existir com nome antigo)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'comentarios' AND column_name = 'professor_professor_id'
    ) THEN
        ALTER TABLE comentarios RENAME COLUMN professor_professor_id TO professor_id;
    END IF;
END $$;

