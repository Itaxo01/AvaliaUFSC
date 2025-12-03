-- =============================================
-- V2: Limpar colunas duplicadas do Hibernate
-- =============================================
-- O Hibernate criou colunas duplicadas com nomes diferentes.
-- Esta migração remove as colunas duplicadas.

-- =============================================
-- TABELA: avaliacoes
-- =============================================

-- Remover colunas duplicadas geradas pelo Hibernate (se existirem)
-- O Hibernate gerou disciplina_disciplina_id além de disciplina_id
-- e professor_professor_id além de professor_id
ALTER TABLE avaliacoes DROP COLUMN IF EXISTS disciplina_disciplina_id;
ALTER TABLE avaliacoes DROP COLUMN IF EXISTS professor_professor_id;

