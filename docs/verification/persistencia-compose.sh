#!/usr/bin/env bash
# Ficha PL 05, exercício 3: os dados mantêm-se depois de recriar os containers do Docker Compose.
# O Rui cria um projeto pela API; os containers são removidos (docker compose down, sem -v, por isso
# o volume fica) e criados de novo; o Rui entra outra vez e o projeto continua lá. No fim, o projeto
# é eliminado (eliminação lógica, RN32), para a base de dados de demonstração ficar como estava.
#
# Uso, a partir da raiz do repositório, com a aplicação do Compose a correr com o perfil demo:
#   bash docs/verification/persistencia-compose.sh 2>&1 | tee docs/verification/persistencia-compose.txt
# O nome do projeto usa só caracteres ASCII (ver autorizacao-demo.sh).
set -u
BASE="${MATRIZ_BASE_URL:-http://localhost:8080}"
API="$BASE/api"
PASSWORD="${MATRIZ_DEMO_PASSWORD:-matriz-demo-2026}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

json() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const o=JSON.parse(s);console.log(($1)(o))})"; }
token() { awk '$6=="XSRF-TOKEN"{print $7}' "$WORK/jar" | tail -1; }
ready() {
  until curl -fsS "$BASE/actuator/health/readiness" >/dev/null 2>&1; do sleep 3; done
  echo "Readiness: $(curl -s "$BASE/actuator/health/readiness")"
}
login() {
  rm -f "$WORK/jar"
  curl -s -c "$WORK/jar" -b "$WORK/jar" "$API/session" -o /dev/null
  curl -s -c "$WORK/jar" -b "$WORK/jar" -H "X-XSRF-TOKEN: $(token)" -H 'Content-Type: application/json' \
    -d "{\"email\":\"rui@empresa.example\",\"password\":\"$PASSWORD\"}" "$API/session" -o /dev/null \
    -w "Entrar como rui@empresa.example: %{http_code}\n"
}
call() { # $1 = método, $2 = caminho, $3 = corpo (opcional); escreve o código HTTP e guarda o corpo
  local args=(-s -c "$WORK/jar" -b "$WORK/jar" -X "$1" -H "X-XSRF-TOKEN: $(token)" -o "$WORK/body")
  [ -n "${3:-}" ] && args+=(-H 'Content-Type: application/json' -d "$3")
  curl "${args[@]}" -w '%{http_code}' "$API$2"
}

NAME="Persistencia $(date +%Y%m%d-%H%M%S)"
echo "== Persistência com o Docker Compose ($(date '+%d/%m/%Y %H:%M'))"
ready
login
TEAM=$(curl -s -b "$WORK/jar" "$API/teams" | json 'o=>o.items.find(t=>t.name.startsWith("Log")).id')
CODE=$(call POST /projects "{\"teamId\":$TEAM,\"name\":\"$NAME\",\"startDate\":\"$(date +%F)\"}")
ID=$(json 'o=>o.id' < "$WORK/body")
echo "Criar o projeto \"$NAME\": $CODE (id $ID)"
echo

echo "== Recriar os containers (docker compose down, sem -v)"
docker compose down
echo "Containers depois do down: $(docker compose ps -a -q | wc -l)"
echo "Volume: $(docker volume ls -q --filter name=matriz_matriz-data)"
docker compose up -d
ready
echo

echo "== Consultar o projeto depois de recriar (sessão nova: a anterior terminou com o container, DA26)"
login
CODE=$(call GET "/projects/$ID")
echo "GET /projects/$ID: $CODE, nome: $(json 'o=>o.name' < "$WORK/body")"
echo

echo "== Limpeza: eliminar o projeto de teste"
echo "DELETE /projects/$ID: $(call DELETE "/projects/$ID")"
