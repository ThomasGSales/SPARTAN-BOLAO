# =====================================================================
#  Frontend Angular (NX) — build multi-stage servido por Nginx.
#  APP_NAME = nome da app no NX (gerada na Fase 3 como "spartan").
# =====================================================================
ARG APP_NAME=spartan

# ---- Stage 1: build ----
# node:24 = npm 11, mesma major que gerou o package-lock.json local.
# (node:20 traz npm 10, que lê o lock como fora de sincronia e quebra o `npm ci`.)
FROM node:24-alpine AS build
# ARG global (antes do 1º FROM) não vale dentro do stage — re-declarar aqui,
# senão ${APP_NAME} fica vazio e o `nx build` cai no projeto raiz.
ARG APP_NAME=spartan
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx nx build ${APP_NAME} --configuration=production

# ---- Stage 2: runtime ----
FROM nginx:1.27-alpine
ARG APP_NAME=spartan
COPY --from=build /app/dist/apps/${APP_NAME}/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
