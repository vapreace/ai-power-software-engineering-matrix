#!/usr/bin/env bash
# Ficha PL 05, exercício 6: simulação controlada da base de dados indisponível (demonstração de
# troubleshooting do Módulo 5). Só no ambiente local, com o Docker Compose: nunca no ambiente publicado.
# Para o PostgreSQL, observa a liveness, a readiness, uma operação real (entrar como o Rui) e os logs,
# e volta a arrancar o PostgreSQL. Não apaga dados: docker compose stop mantém o container e o volume.
#
# Uso, a partir da raiz do repositório, com a aplicação do Compose a correr com o perfil demo:
#   bash docs/verification/incidente-bd.sh 2>&1 | tee docs/verification/incidente-bd.txt
# O corpo do pedido usa só caracteres ASCII (ver autorizacao-demo.sh).
set -u
BASE="${MATRIZ_BASE_URL:-http://localhost:8080}"
PASSWORD="${MATRIZ_DEMO_PASSWORD:-matriz-demo-2026}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

probe() {
  for p in liveness readiness; do
    printf '%-10s %s\n' "$p:" "$(curl -s -m 45 -w ' (HTTP %{http_code}, %{time_total} s)' "$BASE/actuator/health/$p")"
  done
}
login() { # operação que precisa da base de dados: entrar como o Rui
  rm -f "$WORK/jar"
  curl -s -m 45 -c "$WORK/jar" -b "$WORK/jar" "$BASE/api/session" -o /dev/null
  local token; token=$(awk '$6=="XSRF-TOKEN"{print $7}' "$WORK/jar" | tail -1)
  curl -s -m 45 -c "$WORK/jar" -b "$WORK/jar" -H "X-XSRF-TOKEN: $token" -H 'Content-Type: application/json' \
    -d "{\"email\":\"rui@empresa.example\",\"password\":\"$PASSWORD\"}" "$BASE/api/session" \
    -o "$WORK/body" -w 'Entrar como Rui: HTTP %{http_code}'
  echo " $(cut -c1-120 < "$WORK/body")"
}
services() { docker compose ps --format 'table {{.Service}}\t{{.State}}\t{{.Status}}'; }

echo "== 1. Estado inicial ($(date '+%d/%m/%Y %H:%M:%S'))"
services; probe; login

echo; echo "== 2. Falha controlada: parar o PostgreSQL ($(date '+%H:%M:%S'))"
docker compose stop postgres
services

echo; echo "== 3. Investigar ($(date '+%H:%M:%S'))"
probe; login
echo "-- A aplicação chega à porta 5432 do serviço postgres?"
docker compose exec -T app sh -c 'nc -z -w 3 postgres 5432 && echo "Sim" || echo "Não: sem ligação ao postgres:5432"'
echo "-- Avisos e erros da aplicação nos últimos 2 minutos (os 5 mais recentes, cortados)"
docker compose logs --since=2m --no-log-prefix app | grep -E '"level":"(WARN|ERROR)"' | tail -n 5 | cut -c1-260

echo; echo "== 4. Recuperar: arrancar o PostgreSQL ($(date '+%H:%M:%S'))"
docker compose start postgres
until [ "$(docker inspect -f '{{.State.Health.Status}}' matriz-postgres-1)" = healthy ]; do sleep 2; done
services; probe; login
echo; echo "== Fim ($(date '+%H:%M:%S'))"
