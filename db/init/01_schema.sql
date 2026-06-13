-- =====================================================================
--  La Bendita Copa do Mundo SPARTAN — Schema inicial
--  Este script roda AUTOMATICAMENTE na primeira subida do container
--  Postgres (volume /docker-entrypoint-initdb.d).
-- =====================================================================

-- Extensão para UUIDs (ids opacos, melhores que serial para APIs públicas)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------
--  ENUMs
-- ---------------------------------------------------------------------
CREATE TYPE user_role   AS ENUM ('USER', 'ADMIN');
-- Copa 2026: 48 seleções → existe a fase de 16-avos (ROUND_OF_32) antes das oitavas.
CREATE TYPE match_phase AS ENUM ('GROUP', 'ROUND_OF_32', 'ROUND_OF_16', 'QUARTER', 'SEMI', 'THIRD_PLACE', 'FINAL');
CREATE TYPE match_status AS ENUM ('SCHEDULED', 'LIVE', 'FINISHED', 'CANCELLED');

-- ---------------------------------------------------------------------
--  USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120)  NOT NULL,
    email         VARCHAR(180)  NOT NULL UNIQUE,
    password_hash VARCHAR(255),                       -- NULL para usuários só-Google
    google_id     VARCHAR(120) UNIQUE,                -- NULL para usuários tradicionais
    avatar_url    TEXT,
    role          user_role     NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- garante que pelo menos uma forma de autenticação exista
    CONSTRAINT chk_auth_method CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)
);

-- ---------------------------------------------------------------------
--  TEAMS (seleções)
-- ---------------------------------------------------------------------
CREATE TABLE teams (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(80) NOT NULL UNIQUE,          -- "Brasil"
    code        VARCHAR(3)  NOT NULL UNIQUE,          -- "BRA" (padrão FIFA)
    flag_url    TEXT,
    external_id BIGINT UNIQUE                         -- id na football-data.org (sync)
);

-- ---------------------------------------------------------------------
--  MATCHES (jogos)
-- ---------------------------------------------------------------------
CREATE TABLE matches (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_team_id   UUID NOT NULL REFERENCES teams(id),
    away_team_id   UUID NOT NULL REFERENCES teams(id),
    phase          match_phase  NOT NULL,
    group_label    VARCHAR(2),                        -- "A".."H", NULL no mata-mata
    match_datetime TIMESTAMPTZ  NOT NULL,             -- usado para travar palpites
    home_score     INTEGER,                           -- NULL até o jogo acabar
    away_score     INTEGER,
    status         match_status NOT NULL DEFAULT 'SCHEDULED',
    external_id    BIGINT UNIQUE,                      -- id na football-data.org (sync)
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_diff_teams CHECK (home_team_id <> away_team_id)
);

CREATE INDEX idx_matches_datetime ON matches (match_datetime);
CREATE INDEX idx_matches_phase    ON matches (phase);

-- ---------------------------------------------------------------------
--  GUESSES (palpites)
-- ---------------------------------------------------------------------
CREATE TABLE guesses (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    match_id         UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    home_score_guess INTEGER NOT NULL CHECK (home_score_guess >= 0),
    away_score_guess INTEGER NOT NULL CHECK (away_score_guess >= 0),
    points_earned    INTEGER NOT NULL DEFAULT 0,         -- calculado quando o jogo termina
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- REGRA DE OURO: um palpite por usuário por jogo
    CONSTRAINT uq_user_match UNIQUE (user_id, match_id)
);

CREATE INDEX idx_guesses_user  ON guesses (user_id);
CREATE INDEX idx_guesses_match ON guesses (match_id);

-- ---------------------------------------------------------------------
--  RANKING (VIEW derivada — sempre atualizada, zero manutenção)
-- ---------------------------------------------------------------------
CREATE VIEW v_ranking AS
SELECT
    u.id                                              AS user_id,
    u.name,
    u.avatar_url,
    COALESCE(SUM(g.points_earned), 0)                 AS total_points,
    COUNT(g.id) FILTER (WHERE g.points_earned = 5)    AS exact_hits,
    COUNT(g.id) FILTER (WHERE g.points_earned > 0)    AS total_hits,
    COUNT(g.id)                                       AS total_guesses,
    RANK() OVER (ORDER BY COALESCE(SUM(g.points_earned), 0) DESC) AS position
FROM users u
LEFT JOIN guesses g ON g.user_id = u.id
GROUP BY u.id, u.name, u.avatar_url
ORDER BY total_points DESC, exact_hits DESC;

-- ---------------------------------------------------------------------
--  Trigger utilitário: mantém updated_at em dia
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated   BEFORE UPDATE ON users   FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_guesses_updated BEFORE UPDATE ON guesses FOR EACH ROW EXECUTE FUNCTION set_updated_at();
