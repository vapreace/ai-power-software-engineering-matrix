package pt.matriz.projects.persistence;

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

import pt.matriz.projects.ProjectStatus;

/** Tabela {@code projects} (database.md, secção 3.4). A versão é o bloqueio otimista de DA05. */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false, updatable = false)
    private Long teamId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Project() {
    }

    /** Projeto novo: fica "Ativo" (RN07). A equipa não muda depois (RN21). */
    public static Project create(long teamId, long managerId, String name, String description,
            LocalDate startDate, LocalDate expectedEndDate) {
        Project project = new Project();
        project.teamId = teamId;
        project.managerId = managerId;
        project.name = name;
        project.description = description;
        project.startDate = startDate;
        project.expectedEndDate = expectedEndDate;
        project.status = ProjectStatus.ACTIVE;
        return project;
    }

    public void edit(String name, String description, LocalDate startDate, LocalDate expectedEndDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.expectedEndDate = expectedEndDate;
    }

    /** Eliminação lógica (RN31, RN32, DA29): a linha fica guardada. */
    public void markDeleted(OffsetDateTime at) {
        this.deletedAt = at;
    }

    public ProjectRow toRow() {
        return new ProjectRow(id, teamId, managerId, name, description, startDate, expectedEndDate, status, version);
    }

    public Long getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getExpectedEndDate() {
        return expectedEndDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
