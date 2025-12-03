-- =============================================
-- V1: Initial Schema - AvaliaUFSC Database
-- =============================================
-- Este script cria todas as tabelas iniciais do sistema

-- =============================================
-- TABELA: usuarios
-- =============================================
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    matricula VARCHAR(255) UNIQUE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

-- =============================================
-- TABELA: disciplinas
-- =============================================
CREATE TABLE IF NOT EXISTS disciplinas (
    disciplina_id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nome VARCHAR(300) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_disciplinas_codigo ON disciplinas(codigo);

-- =============================================
-- TABELA: professores
-- =============================================
CREATE TABLE IF NOT EXISTS professores (
    professor_id VARCHAR(50) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

-- =============================================
-- TABELA: professor_disciplina (associação N:N)
-- =============================================
CREATE TABLE IF NOT EXISTS professor_disciplina (
    professor_id VARCHAR(50) NOT NULL,
    disciplina_id BIGINT NOT NULL,
    ultimo_semestre VARCHAR(50),
    PRIMARY KEY (professor_id, disciplina_id),
    CONSTRAINT fk_pd_professor FOREIGN KEY (professor_id) 
        REFERENCES professores(professor_id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_disciplina FOREIGN KEY (disciplina_id) 
        REFERENCES disciplinas(disciplina_id) ON DELETE CASCADE
);

-- =============================================
-- TABELA: avaliacoes
-- =============================================
CREATE TABLE IF NOT EXISTS avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    professor_id VARCHAR(50) NOT NULL,
    disciplina_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    nota INTEGER NOT NULL CHECK (nota >= 1 AND nota <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacao_professor FOREIGN KEY (professor_id) 
        REFERENCES professores(professor_id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_disciplina FOREIGN KEY (disciplina_id) 
        REFERENCES disciplinas(disciplina_id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_usuario FOREIGN KEY (user_email) 
        REFERENCES usuarios(email) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_avaliacao_unique 
    ON avaliacoes(disciplina_id, user_email, professor_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_disciplina ON avaliacoes(disciplina_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_professor ON avaliacoes(professor_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_user ON avaliacoes(user_email);

-- =============================================
-- TABELA: comentarios
-- =============================================
CREATE TABLE IF NOT EXISTS comentarios (
    comentario_id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    texto VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    edited_at TIMESTAMP,
    up_votes INTEGER DEFAULT 0,
    down_votes INTEGER DEFAULT 0,
    alarmante BOOLEAN NOT NULL DEFAULT FALSE,
    denunciado BOOLEAN NOT NULL DEFAULT FALSE,
    denuncias_count INTEGER DEFAULT 0,
    disciplina_id BIGINT NOT NULL,
    professor_id VARCHAR(50) NOT NULL,
    pai_id BIGINT,
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (user_email) 
        REFERENCES usuarios(email) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_disciplina FOREIGN KEY (disciplina_id) 
        REFERENCES disciplinas(disciplina_id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_professor FOREIGN KEY (professor_id) 
        REFERENCES professores(professor_id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_pai FOREIGN KEY (pai_id) 
        REFERENCES comentarios(comentario_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comentario_pai ON comentarios(pai_id);
CREATE INDEX IF NOT EXISTS idx_comentario_user_created ON comentarios(user_email, created_at);
CREATE INDEX IF NOT EXISTS idx_comentario_created ON comentarios(created_at);

-- =============================================
-- TABELA: comentario_votes
-- =============================================
CREATE TABLE IF NOT EXISTS comentario_votes (
    comentario_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    is_upvote BOOLEAN NOT NULL,
    PRIMARY KEY (comentario_id, user_email),
    CONSTRAINT fk_vote_comentario FOREIGN KEY (comentario_id) 
        REFERENCES comentarios(comentario_id) ON DELETE CASCADE
);

-- =============================================
-- TABELA: comentario_denuncias
-- =============================================
CREATE TABLE IF NOT EXISTS comentario_denuncias (
    comentario_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    PRIMARY KEY (comentario_id, user_email),
    CONSTRAINT fk_denuncia_comentario FOREIGN KEY (comentario_id) 
        REFERENCES comentarios(comentario_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_denuncias_comentario ON comentario_denuncias(comentario_id);

-- =============================================
-- TABELA: arquivos_comentario
-- =============================================
CREATE TABLE IF NOT EXISTS arquivos_comentario (
    id BIGSERIAL PRIMARY KEY,
    nome_original VARCHAR(255) NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(100),
    tamanho BIGINT,
    caminho_arquivo VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    comentario_id BIGINT NOT NULL,
    CONSTRAINT fk_arquivo_comentario FOREIGN KEY (comentario_id) 
        REFERENCES comentarios(comentario_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_arquivos_comentario ON arquivos_comentario(comentario_id);

-- =============================================
-- TABELA: scrapper_status
-- =============================================
CREATE TABLE IF NOT EXISTS scrapper_status (
    id BIGSERIAL PRIMARY KEY,
    ultima_execucao TIMESTAMP,
    ultimo_sucesso TIMESTAMP,
    executando BOOLEAN DEFAULT FALSE,
    disciplinas_capturadas INTEGER DEFAULT 0,
    professores_capturados INTEGER DEFAULT 0,
    ultimo_erro VARCHAR(1000),
    ultimo_administrador VARCHAR(150),
    total_execucoes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    data_hash VARCHAR(64)
);

-- =============================================
-- TABELA: usuarios_banidos
-- =============================================
CREATE TABLE IF NOT EXISTS usuarios_banidos (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    banido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    banido_por VARCHAR(255),
    motivo VARCHAR(500)
);
