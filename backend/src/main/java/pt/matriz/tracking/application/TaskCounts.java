package pt.matriz.tracking.application;

import pt.matriz.tasks.TaskTotals;

/** Número de tarefas por estado (TaskCounts em openapi.yaml, RN33). */
public record TaskCounts(long todo, long inProgress, long done) {

    static TaskCounts of(TaskTotals totals) {
        return new TaskCounts(totals.todo(), totals.inProgress(), totals.done());
    }

    TaskCounts plus(TaskCounts other) {
        return new TaskCounts(todo + other.todo, inProgress + other.inProgress, done + other.done);
    }
}
