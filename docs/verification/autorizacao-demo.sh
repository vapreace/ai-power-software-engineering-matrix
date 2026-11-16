#!/usr/bin/env bash
# Ficha PL 04, exercício 5: verificação de autorização com duas contas de demonstração (IDOR / Broken
# Access Control). Corre contra o backend com o perfil demo (README.md), numa base de dados de
# demonstração acabada de criar. O Pedro (Membro da Logística Norte) tenta ler e alterar recursos da
# equipa Compras, que não vê; a Ana (Administrador) só serve para obter os identificadores e confirmar
# no fim que nada mudou. Não altera dados: todos os pedidos do Pedro devem ser recusados.
#
# Uso, a partir da raiz do repositório:
#   bash docs/verification/autorizacao-demo.sh | tee docs/verification/autorizacao-demo.txt
# Os corpos dos pedidos usam só caracteres ASCII: no Git Bash em Windows, os acentos nos argumentos
# do curl podem chegar com outra codificação e o JSON é recusado com 400 antes da verificação de acesso.
set -u
API="${MATRIZ_API:-http://localhost:8080/api}"
PASSWORD="${MATRIZ_DEMO_PASSWORD:-matriz-demo-2026}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

json() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const o=JSON.parse(s);console.log(($1)(o))})"; }
token() { awk '$6=="XSRF-TOKEN"{print $7}' "$WORK/$1.jar" | tail -1; }

login() { # $1 = nome da sessão, $2 = email
  curl -s -c "$WORK/$1.jar" -b "$WORK/$1.jar" "$API/session" -o /dev/null
  curl -s -c "$WORK/$1.jar" -b "$WORK/$1.jar" -H "X-XSRF-TOKEN: $(token "$1")" \
    -H 'Content-Type: application/json' -d "{\"email\":\"$2\",\"password\":\"$PASSWORD\"}" \
    "$API/session" -o /dev/null -w "Entrar como $2: %{http_code}\n"
}

call() { # $1 = sessão, $2 = método, $3 = caminho, $4 = corpo (opcional)
  local args=(-s -c "$WORK/$1.jar" -b "$WORK/$1.jar" -X "$2" -H "X-XSRF-TOKEN: $(token "$1")" -o "$WORK/body")
  [ -n "${4:-}" ] && args+=(-H 'Content-Type: application/json' -d "$4")
  local code; code=$(curl "${args[@]}" -w '%{http_code}' "$API$3")
  printf '%-7s %-48s -> %s %s\n' "$2" "$3" "$code" "$(tr -d '\n' < "$WORK/body" | cut -c1-140)"
}

get() { curl -s -b "$WORK/$1.jar" "$API$2"; }

echo "== Verificação de autorização com contas de demonstração ($(date '+%d/%m/%Y %H:%M'))"
login ana ana@empresa.example
login pedro pedro@empresa.example

PROJECT=$(get ana "/projects?search=Fornecedores" | json 'o=>o.items[0].id')
TASK=$(get ana "/projects/$PROJECT/tasks" | json 'o=>o.items.find(t=>t.title==="Avaliar fornecedores atuais").id')
TEAM=$(get ana "/teams" | json 'o=>o.items.find(t=>t.name==="Compras").id')
USERS=$(get ana "/users")
JOAO=$(echo "$USERS" | json 'o=>o.items.find(u=>u.email==="joao@empresa.example").id')
MARTA=$(echo "$USERS" | json 'o=>o.items.find(u=>u.email==="marta@empresa.example").id')
RUI=$(echo "$USERS" | json 'o=>o.items.find(u=>u.email==="rui@empresa.example").id')
BEFORE=$(get ana "/tasks/$TASK")
echo "Fornecedores=$PROJECT, tarefa 'Avaliar fornecedores atuais'=$TASK (responsável: João), Compras=$TEAM"
echo "João=$JOAO, Marta=$MARTA, Rui=$RUI"
echo

echo "== O Pedro tenta ler e alterar recursos da equipa Compras (esperado: 403 em todos)"
call pedro GET    "/projects/$PROJECT"
call pedro GET    "/projects/$PROJECT/tasks"
call pedro GET    "/tasks/$TASK"
call pedro PATCH  "/tasks/$TASK" '{"title":"Alterado pelo Pedro","version":0}'
call pedro PATCH  "/tasks/$TASK" '{"title":"","version":0}'
call pedro PUT    "/tasks/$TASK/status" '{"status":"DONE","version":0}'
call pedro DELETE "/tasks/$TASK"
call pedro GET    "/tasks/$TASK/comments"
call pedro POST   "/tasks/$TASK/comments" '{"content":"Comentario do Pedro"}'
call pedro PUT    "/tasks/$TASK/assignees/$MARTA"
call pedro POST   "/projects/$PROJECT/tasks" '{"title":"Tarefa do Pedro"}'
echo

echo "== Respostas que podem revelar informação de recursos não visíveis (comparar os códigos)"
echo "-- Responsáveis de uma tarefa que o Pedro não vê: o João é responsável, a Marta não"
call pedro DELETE "/tasks/$TASK/assignees/$JOAO"
call pedro DELETE "/tasks/$TASK/assignees/$MARTA"
echo "-- Pessoas de uma equipa a que o Pedro não pertence: o João pertence a Compras, o Rui não"
call pedro DELETE "/teams/$TEAM/members/$JOAO"
call pedro DELETE "/teams/$TEAM/members/$RUI"
echo

echo "== A Ana confirma que a tarefa não mudou"
AFTER=$(get ana "/tasks/$TASK")
if [ "$BEFORE" = "$AFTER" ]; then echo "Tarefa igual antes e depois: sim"; else echo "Tarefa igual antes e depois: NÃO"; echo "$BEFORE"; echo "$AFTER"; fi
echo "Comentários da tarefa: $(get ana "/tasks/$TASK/comments" | json 'o=>o.items.length')"
echo "Pessoas de Compras: $(get ana "/teams/$TEAM/members" | json 'o=>o.items.map(m=>m.name).join(", ")')"
