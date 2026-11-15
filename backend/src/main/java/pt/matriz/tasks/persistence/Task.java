package pt.matriz.tasks.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;

/** Tabela {@code tasks} (database.md, secção 3.5). A versão é o bloqueio otimista de DA05. */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Task() {
    }

    /** Tarefa nova: fica "Por fazer" e sem responsáveis (RN12, US05). */
    public static Task create(long projectId, String title, String description, LocalDate dueDate, TaskPriority priority) {
        Task task = new Task();
        task.projectId = projectId;
        task.title = title;
        task.description = description;
        task.dueDate = dueDate;
        task.priority = priority;
        task.status = TaskStatus.TODO;
        return task;
    }

    public void edit(String title, String description, LocalDate dueDate, TaskPriority priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    /** Qualquer mudança entre os três estados é permitida (RN12). */
    public void changeStatus(TaskStatus status) {
        this.status = status;
    }

    /** Eliminação lógica (RN19, RN32, DA29): a linha fica guardada. */
    public void markDeleted(OffsetDateTime at) {
        this.deletedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
