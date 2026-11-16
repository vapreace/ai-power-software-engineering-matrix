#!/usr/bin/env python3
"""Quality gate da cobertura de código (DA37, DA39; docs/verification.md, secção 14.2).

Lê dois relatórios CSV do JaCoCo: o dos testes unitários e o total (unitários e integração).

"Regras de negócio" são os serviços, os comandos, os listeners e o pacote common, menos a
infraestrutura. "Infraestrutura" são as consultas SQL e os controllers e filtros (pacotes
persistence e api), o encaminhamento do frontend, o tratamento de erros e o acesso à sessão
HTTP: só fazem sentido com a base de dados e o Spring e ficam cobertos pela integração.

Escreve um resumo em Markdown (vai para o resumo da execução no GitHub) e termina com erro se
as branches das regras de negócio não estiverem todas cobertas pelos testes unitários.
Uso: coverage_gate.py <csv dos unitários> <csv total>
"""
import csv
import sys

INFRASTRUCTURE_PACKAGES = (".api", ".persistence")
INFRASTRUCTURE_CLASSES = ("SpaFallbackConfiguration", "GlobalExceptionHandler", "SessionCurrentUserProvider")


def is_infrastructure(row):
    # As classes anónimas e internas aparecem como "Externa.Interna" ou "Externa.new X() {...}".
    outer_class = row["CLASS"].split(".")[0]
    return row["PACKAGE"].endswith(INFRASTRUCTURE_PACKAGES) or outer_class in INFRASTRUCTURE_CLASSES


def branches(path):
    """Branches cobertas e totais por âmbito: regras, infraestrutura e backend todo."""
    result = {"rules": [0, 0], "infrastructure": [0, 0], "all": [0, 0]}
    with open(path, newline="", encoding="utf-8") as report:
        for row in csv.DictReader(report):
            covered, missed = int(row["BRANCH_COVERED"]), int(row["BRANCH_MISSED"])
            scope = "infrastructure" if is_infrastructure(row) else "rules"
            for key in (scope, "all"):
                result[key][0] += covered
                result[key][1] += covered + missed
    return result


def percent(covered, total):
    return f"{covered * 100 // total}% ({covered} de {total})" if total else "sem branches"


def main(unit_csv, total_csv):
    unit, total = branches(unit_csv), branches(total_csv)
    labels = {"rules": "Regras de negócio", "infrastructure": "Infraestrutura", "all": "Backend todo"}
    print("## Cobertura de código (JaCoCo, branches)\n")
    print("| Âmbito | Só testes unitários | Unitários e integração |")
    print("|--------|---------------------|------------------------|")
    for key, label in labels.items():
        print(f"| {label} | {percent(*unit[key])} | {percent(*total[key])} |")
    covered, all_rules = unit["rules"]
    passed = all_rules > 0 and covered == all_rules
    print(f"\nQuality gate (DA37): todas as branches das regras de negócio cobertas pelos testes "
          f"unitários: **{'aprovado' if passed else 'reprovado'}**.")
    if not passed:
        print(f"Quality gate reprovado: {all_rules - covered} branches das regras de negócio sem teste unitário.",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
