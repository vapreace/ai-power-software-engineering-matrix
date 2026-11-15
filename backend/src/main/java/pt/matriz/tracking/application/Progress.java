package pt.matriz.tracking.application;

import pt.matriz.tasks.TaskTotals;

/**
 * Progresso do projeto (requirements.md, secção 2): concluídas ÷ total × 100, arredondado
 * para baixo. Sem tarefas, {@code percentage} é {@code null} e a interface mostra "Sem tarefas" (E1).
 */
public record Progress(long completedTasks, long totalTasks, Integer percentage) {

    static Progress of(TaskTotals totals) {
        Integer percentage = totals.total() == 0 ? null : (int) (totals.done() * 100 / totals.total());
        return new Progress(totals.done(), totals.total(), percentage);
    }
}
