# ⚔️ La Bendita Copa do Mundo SPARTAN

Sistema de bolões e palpites para a Copa do Mundo. Foco em velocidade de
preenchimento, UI futurista/minimalista (vermelho Spartan + dark mode) e deploy
100% containerizado.

## Stack
- **Frontend:** Angular (NX monorepo) + Angular Material + TailwindCSS
- **Backend:** Kotlin + Spring Boot
- **Banco:** PostgreSQL 16
- **Auth:** Spring Security (JWT) + OAuth2 (Google)
- **Infra:** Docker + Docker Compose

## Estrutura do repositório (polyglot monorepo)

```
SpartanBolão/
├── docker-compose.yml          # Orquestra db + backend + frontend
├── .env.example                # Variáveis de ambiente (copie para .env)
├── .gitignore
│
├── db/
│   └── init/
│       └── 01_schema.sql       # Schema rodado na 1ª subida do Postgres
│
├── backend/                    # === Spring Boot (Kotlin) ===
│   ├── Dockerfile
│   └── src/main/kotlin/com/spartan/bolao/
│       ├── BolaoApplication.kt         # (Fase 2) entrypoint
│       ├── domain/                     # Entidades JPA
│       │   ├── User.kt  Team.kt  Match.kt  Guess.kt  Enums.kt
│       ├── repository/                 # (Fase 2) Spring Data interfaces
│       ├── service/                    # (Fase 2) regras de negócio + scoring
│       ├── web/                        # (Fase 2) Controllers REST + DTOs
│       ├── security/                   # (Fase 2) JWT + OAuth2
│       └── config/                     # (Fase 2) beans, CORS, etc.
│
└── frontend/                   # === NX workspace (Angular) ===
    ├── Dockerfile
    ├── nginx.conf
    └── apps/spartan/           # (Fase 3) app principal
        └── libs/               # (Fase 3) feature libs: auth, matches, ranking
```

### Por que `backend/` e `frontend/` separados, e não tudo dentro do NX?
NX é excelente orquestrando projetos JS/TS, mas Spring Boot/Gradle vive melhor
fora dele. Mantê-los como pastas irmãs dá um repo poliglota limpo, com builds e
Dockerfiles independentes — e ainda assim um único `docker compose up`.

## Como rodar (estado atual — só o banco está pronto)
```bash
cp .env.example .env
docker compose up -d db        # sobe Postgres com o schema já criado
```
`backend` e `frontend` são placeholders; serão preenchidos nas Fases 2 e 3.

## Sistema de pontuação
| Acerto | Pontos |
|---|---|
| Placar exato | 5 |
| Vencedor + saldo de gols | 3 |
| Apenas o vencedor | 1 |
| Errou | 0 |

## Roadmap de fases
- [x] **Fase 0 — Fundação:** schema, entidades, Docker, estrutura *(você está aqui)*
- [ ] **Fase 1 — Primeira API + componente:** scaffold Spring Boot + NX, CRUD de jogos
- [ ] **Fase 2 — Auth:** JWT + Google OAuth2
- [ ] **Fase 3 — Match Engine (UI):** tela de palpites rápida com auto-save
- [ ] **Fase 4 — Scoring + Ranking:** motor de pontos + leaderboard em tempo real
```

## Bootstrap das próximas fases (referência)
```bash
# Frontend (Fase 1/3) — dentro de ./frontend
npx create-nx-workspace@latest . --preset=angular-monorepo --appName=spartan \
  --style=scss --routing --e2eTestRunner=none
npm install @angular/material tailwindcss

# Backend (Fase 1/2) — gere via https://start.spring.io
# Dependencies: Web, Data JPA, PostgreSQL Driver, Security, OAuth2 Resource Server,
#               OAuth2 Client, Validation, Kotlin
```
