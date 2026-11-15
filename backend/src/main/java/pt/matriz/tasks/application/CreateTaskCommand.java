package pt.matriz.tasks.application;

import java.time.LocalDate;

import pt.matriz.tasks.TaskPriority;

/** Dados para criar uma tarefa (CreateTaskRequest em openapi.yaml, US05). */
public record CreateTaskCommand(String title, String description, LocalDate dueDate, TaskPriority priority) {
}
