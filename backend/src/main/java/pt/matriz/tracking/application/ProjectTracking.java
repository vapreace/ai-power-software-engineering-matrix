package pt.matriz.tracking.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import pt.matriz.common.CurrentUser;
import pt.matriz.projects.CreateProjectCommand;
import pt.matriz.projects.ProjectStatus;
import pt.matriz.projects.ProjectView;
import pt.matriz.projects.Projects;
import pt.matriz.projects.UpdateProjectCommand;
import pt.matriz.tasks.TaskTotals;
import pt.matriz.tasks.Tasks;

/**
 * Junta os dados do projeto (módulo Projetos) com as contagens das tarefas (módulo Tarefas)
 * para a lista, o detalhe e o painel inicial (architecture.md, secção 5.1). Não guarda dados: criar, editar e
 * eliminar são decididos e gravados pelo módulo Projetos, com as suas regras e permissões;
 * este serviço só acrescenta o progresso e os atrasos à resposta, como pede openapi.yaml.
 */
@Service
public class ProjectTracking {

    private final Projects projects;
    private final Tasks tasks;

    ProjectTracking(Projects projects, Tasks tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    public List<ProjectSummary> list(CurrentUser user, Set<ProjectStatus> statuses, String query) {
        List<ProjectView> found = projects.search(user, statuses, query);
        Map<Long, TaskTotals> totals = tasks.totalsByProject(found.stream().map(ProjectView::id).toList());
        return found.stream().map(project -> summary(project, totals.getOrDefault(project.id(), TaskTotals.NONE))).toList();
    }

    /**
     * Painel inicial (US21, RN33): os mesmos projetos que a lista mostra por omissão (sem os
     * "Arquivado", RN25), pela mesma ordem, com as tarefas por estado e os totais desses projetos.
     * As contagens incluem todas as tarefas não eliminadas (RN32).
     */
    public Dashboard dashboard(CurrentUser user) {
        List<ProjectView> found = projects.search(user, null, null);
        Map<Long, TaskTotals> totals = tasks.totalsByProject(found.stream().map(ProjectView::id).toList());
        List<DashboardProject> items = found.stream().map(project -> {
            TaskTotals projectTotals = totals.getOrDefault(project.id(), TaskTotals.NONE);
            return new DashboardProject(project.id(), project.name(), project.team(), project.manager(),
                    project.status(), Progress.of(projectTotals), TaskCounts.of(projectTotals));
        }).toList();
        TaskCounts sum = items.stream().map(DashboardProject::taskCounts)
                .reduce(new TaskCounts(0, 0, 0), TaskCounts::plus);
        return new Dashboard(items, sum);
    }

    public ProjectDetail get(CurrentUser user, long projectId) {
        return detail(projects.get(user, projectId));
    }

    public ProjectDetail create(CurrentUser user, CreateProjectCommand command) {
        return detail(projects.create(user, command));
    }

    public ProjectDetail update(CurrentUser user, long projectId, UpdateProjectCommand command) {
        return detail(projects.update(user, projectId, command));
    }

    public ProjectDetail changeStatus(CurrentUser user, long projectId, ProjectStatus status, Long version) {
        return detail(projects.changeStatus(user, projectId, status, version));
    }

    public ProjectDetail changeManager(CurrentUser user, long projectId, Long managerId, Long version) {
        return detail(projects.changeManager(user, projectId, managerId, version));
    }

    public void delete(CurrentUser user, long projectId) {
        projects.delete(user, projectId);
    }

    private ProjectDetail detail(ProjectView project) {
        TaskTotals totals = tasks.totalsByProject(List.of(project.id())).getOrDefault(project.id(), TaskTotals.NONE);
        return new ProjectDetail(project.id(), project.name(), project.team(), project.manager(), project.status(),
                project.expectedEndDate(), Progress.of(totals), totals.overdue(), project.description(),
                project.startDate(), project.version());
    }

    private static ProjectSummary summary(ProjectView project, TaskTotals totals) {
        return new ProjectSummary(project.id(), project.name(), project.team(), project.manager(), project.status(),
                project.expectedEndDate(), Progress.of(totals), totals.overdue());
    }
}
