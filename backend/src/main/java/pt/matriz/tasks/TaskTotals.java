package pt.matriz.tasks;

/**
 * Contagens das tarefas não eliminadas de um projeto (database.md, secção 7; RN32).
 * {@code overdue} usa a definição de tarefa atrasada (requirements.md, secção 2).
 */
public record TaskTotals(long total, long todo, long inProgress, long done, long overdue) {

    public static final TaskTotals NONE = new TaskTotals(0, 0, 0, 0, 0);
}
