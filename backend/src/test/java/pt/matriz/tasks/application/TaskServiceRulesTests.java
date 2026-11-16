package pt.matriz.tasks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.ApiFieldError;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.OrganizationClock;
import pt.matriz.common.Patch;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.projects.ProjectAccess;
import pt.matriz.projects.ProjectStatus;
import pt.matriz.projects.Projects;
import pt.matriz.tasks.MyTaskView;
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
import pt.matriz.tasks.TaskView;
import pt.matriz.tasks.persistence.Task;
import pt.matriz.tasks.persistence.TaskQueries;
import pt.matriz.tasks.persistence.TaskRepository;
import pt.matriz.tasks.persistence.TaskRow;
import pt.matriz.teams.Teams;

/**
 * Testes unitários das regras de {@link TaskService} (passo 2b, DA37): permissões de
 * requirements.md secção 3.1, validações (RN11, RN13, RN29), estados do projeto (RN08, RN09),
 * versão (DA05), responsáveis (RN14, RN15) e atrasos (requirements.md secção 2). O serviço é real;
 * a persistência e os outros módulos são mocks. A data atual é 10/11/2026 (Relógio fixo).
 * As permissões chegam já calculadas em {@link ProjectAccess}: os testes variam esse valor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceRulesTests {

    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final long PROJECT = 1L;
    private static final long TEAM = 10L;
    private static final long TASK = 42L;

    private static final CurrentUser RUI = user(2L, "Rui", Role.MANAGER);
    private static final CurrentUser MARTA = user(5L, "Marta", Role.MEMBER);

    @Mock
    private TaskRepository repository;
    @Mock
    private TaskQueries queries;
    @Mock
    private Projects projects;
    @Mock
    private Accounts accounts;
    @Mock
    private Teams teams;

    private OrganizationClock clock;
    private TaskService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE);
        clock = new OrganizationClock(fixed);
        service = new TaskService(repository, queries, projects, accounts, teams, clock);
        // A base de dados daria o identificador ao gravar uma tarefa nova.
        when(repository.saveAndFlush(any(Task.class))).thenAnswer(call -> {
            Task saved = call.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", TASK);
            }
            return saved;
        });
    }

    // ───── Listar (US10, US11) ─────

    @Nested
    class Listing {

        @Test
        void unknownProjectIsNotFound() {
            ApiException error = refused(() -> service.list(RUI, PROJECT, Set.of(), Set.of(), false, null, false));
            assertError(error, HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "O projeto não existe.");
        }

        @Test
        void projectThatIsNotVisibleIsRefused() {
            givenAccess(RUI, false, false, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.list(RUI, PROJECT, Set.of(), Set.of(), false, null, false));
            assertError(error, HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, "Não tem acesso a esta página.");
        }

        @Test
        void assigneeAndUnassignedTogetherAreRefused() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.list(RUI, PROJECT, Set.of(), Set.of(), false, 5L, true));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("unassigned");
        }

        @Test
        void filterByOneAssigneeIsPassedOn() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            service.list(RUI, PROJECT, Set.of(), Set.of(), false, 5L, false);
            verify(queries).list(PROJECT, Set.of(), Set.of(), false, 5L, false, TODAY);
        }

        @Test
        void emptyFilterValuesAreIgnored() {
            // Bug B4: ?status= chega como null dentro do conjunto.
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            Set<TaskStatus> statuses = new HashSet<>(Arrays.asList(TaskStatus.TODO, null));

            service.list(RUI, PROJECT, statuses, null, true, null, false);

            verify(queries).list(PROJECT, Set.of(TaskStatus.TODO), Set.of(), true, null, false, TODAY);
        }

        @Test
        void onlyOpenTasksWithADueDateBeforeTodayAreOverdue() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            when(queries.list(eq(PROJECT), any(), any(), eq(false), isNull(), eq(false), eq(TODAY))).thenReturn(List.of(
                    row(1, TaskStatus.TODO, TODAY.minusDays(1)),
                    row(2, TaskStatus.IN_PROGRESS, TODAY),
                    row(3, TaskStatus.DONE, TODAY.minusDays(5)),
                    row(4, TaskStatus.TODO, null)));

            List<TaskView> tasks = service.list(RUI, PROJECT, Set.of(), Set.of(), false, null, false);

            assertThat(tasks).extracting(TaskView::overdue).containsExactly(true, false, false, false);
        }

        @Test
        void assigneesAreOrderedByNameWithoutAccentsOrCaseAndThenById() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            when(queries.list(eq(PROJECT), any(), any(), eq(false), isNull(), eq(false), eq(TODAY)))
                    .thenReturn(List.of(row(1, TaskStatus.TODO, null)));
            when(queries.assigneeIds(List.of(1L))).thenReturn(Map.of(1L, List.of(3L, 9L, 2L, 5L)));
            when(accounts.findAllById(any())).thenReturn(List.of(account(3, "Bruno"), account(9, "ana"),
                    account(2, "Álvaro"), account(5, "Ana")));

            TaskView task = service.list(RUI, PROJECT, Set.of(), Set.of(), false, null, false).getFirst();

            assertThat(task.assignees()).extracting(Reference::id).containsExactly(2L, 5L, 9L, 3L);
        }
    }

    // ───── As minhas tarefas (US08) e regras automáticas (RN16, RN17) ─────

    @Nested
    class MyTasksAndAutomaticRules {

        @Test
        void myTasksKeepOnlyTasksOfActiveProjectsWithTheProjectName() {
            when(queries.listByAssignee(MARTA.id(), true)).thenReturn(List.of(
                    rowInProject(1, PROJECT), rowInProject(2, 7L)));
            when(projects.activeProjectNames(List.of(PROJECT, 7L))).thenReturn(Map.of(PROJECT, "Novo armazém"));

            List<MyTaskView> tasks = service.myTasks(MARTA, true);

            assertThat(tasks).extracting(MyTaskView::id).containsExactly(1L);
            assertThat(tasks.getFirst().project()).isEqualTo(new Reference(PROJECT, "Novo armazém"));
        }

        @Test
        void deactivationRemovesThePersonFromOpenTasksOfAllProjects() {
            service.removeFromOpenTasks(5L, null);
            verify(queries).removeFromOpenTasks(5L, null, clock.now());
            verify(projects, never()).projectIdsOfTeam(anyLong());
        }

        @Test
        void leavingATeamRemovesThePersonOnlyFromTheProjectsOfThatTeam() {
            when(projects.projectIdsOfTeam(TEAM)).thenReturn(List.of(PROJECT, 3L));
            service.removeFromOpenTasks(5L, TEAM);
            verify(queries).removeFromOpenTasks(5L, List.of(PROJECT, 3L), clock.now());
        }
    }

    // ───── Criar (US05) ─────

    @Nested
    class Creating {

        @Test
        void unknownProjectIsNotFound() {
            ApiException error = refused(() -> service.create(RUI, PROJECT, command("Inventário")));
            assertError(error, HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "O projeto não existe.");
        }

        @Test
        void onlyWhoManagesTheProjectCreatesAndPermissionComesBeforeValidation() {
            givenAccess(MARTA, true, false, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.create(MARTA, PROJECT, command("")));
            assertError(error, HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, "Não tem permissão para esta operação.");
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void descriptionHasAtMost2000Characters() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(service.create(RUI, PROJECT, new CreateTaskCommand("Inventário", "d".repeat(2000), null, null))
                    .description()).hasSize(2000);
            ApiException error = refused(() -> service.create(RUI, PROJECT,
                    new CreateTaskCommand("Inventário", "d".repeat(2001), null, null)));
            assertThat(error.getErrors()).containsExactly(
                    new ApiFieldError("description", "A descrição tem no máximo 2000 caracteres."));
        }

        @Test
        void blankDescriptionIsStoredAsNothing() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(service.create(RUI, PROJECT, new CreateTaskCommand("Inventário", "   ", null, null))
                    .description()).isNull();
        }

        @Test
        void dueDateCannotBeBeforeTodayButTodayIsAccepted() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(service.create(RUI, PROJECT, new CreateTaskCommand("Inventário", null, TODAY, null)).dueDate())
                    .isEqualTo(TODAY);
            ApiException error = refused(() -> service.create(RUI, PROJECT,
                    new CreateTaskCommand("Inventário", null, TODAY.minusDays(1), null)));
            assertThat(error.getErrors()).containsExactly(
                    new ApiFieldError("dueDate", "O prazo não pode ser anterior à data atual"));
        }

        @Test
        void allInvalidFieldsAreReturnedTogether() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.create(RUI, PROJECT,
                    new CreateTaskCommand(" ", "d".repeat(2001), TODAY.minusDays(1), null)));
            assertThat(error.getMessage()).isEqualTo("Há campos inválidos.");
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("title", "description", "dueDate");
        }

        @Test
        void onlyActiveProjectsAcceptNewTasksAndValidationComesFirst() {
            givenAccess(RUI, true, true, ProjectStatus.COMPLETED);
            assertThat(refused(() -> service.create(RUI, PROJECT, command(""))).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            ApiException error = refused(() -> service.create(RUI, PROJECT, command("Inventário")));
            assertError(error, HttpStatus.CONFLICT, ProblemCode.PROJECT_NOT_ACTIVE, "Só projetos ativos aceitam novas tarefas.");
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void indicatedPriorityIsKept() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(service.create(RUI, PROJECT, new CreateTaskCommand("Inventário", null, null, TaskPriority.HIGH))
                    .priority()).isEqualTo(TaskPriority.HIGH);
        }
    }

    // ───── Consultar (US16) ─────

    @Nested
    class Reading {

        @Test
        void deletedOrUnknownTaskIsNotFound() {
            ApiException error = refused(() -> service.get(RUI, TASK));
            assertError(error, HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "A tarefa não existe.");
        }

        @Test
        void taskWhoseProjectNoLongerExistsIsNotFound() {
            givenTask(task(TaskStatus.TODO, null, 0));
            ApiException error = refused(() -> service.get(RUI, TASK));
            assertError(error, HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "A tarefa não existe.");
        }

        @Test
        void taskOfAProjectThatIsNotVisibleIsRefused() {
            givenTask(task(TaskStatus.TODO, null, 0));
            givenAccess(RUI, false, false, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.get(RUI, TASK));
            assertError(error, HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, "Não tem acesso a esta página.");
        }

        @Test
        void visibleTaskIsReturned() {
            givenTask(task(TaskStatus.IN_PROGRESS, TODAY.minusDays(1), 3));
            givenAccess(MARTA, true, false, ProjectStatus.ACTIVE);
            TaskView view = service.get(MARTA, TASK);
            assertThat(view.id()).isEqualTo(TASK);
            assertThat(view.version()).isEqualTo(3);
            assertThat(view.overdue()).isTrue();
        }
    }

    // ───── Editar (US16) ─────

    @Nested
    class Editing {

        @BeforeEach
        void openTask() {
            givenTask(task(TaskStatus.TODO, TODAY.minusDays(2), 1));
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
        }

        @Test
        void onlyWhoManagesTheProjectEditsAndPermissionComesBeforeValidation() {
            givenAccess(MARTA, true, false, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.update(MARTA, TASK, edit(null, Patch.of(""), absent(), absent(), absent())));
            assertError(error, HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, "Não tem permissão para esta operação.");
        }

        @Test
        void fieldsThatAreNotEditedHereAreRefused() {
            UpdateTaskCommand command = new UpdateTaskCommand(1L, absent(), absent(), absent(), absent(), Set.of("status"));
            ApiException error = refused(() -> service.update(RUI, TASK, command));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("status", "Este campo não pode ser alterado."));
        }

        @Test
        void versionIsRequired() {
            ApiException error = refused(() -> service.update(RUI, TASK, edit(null, Patch.of("Novo"), absent(), absent(), absent())));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("version", "A versão é obrigatória."));
        }

        @Test
        void atLeastOneFieldMustChange() {
            ApiException error = refused(() -> service.update(RUI, TASK, edit(1L, absent(), absent(), absent(), absent())));
            assertError(error, HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "Indique pelo menos um campo a alterar.");
        }

        @Test
        void overdueTaskCanBeEditedWithoutChangingTheDueDate() {
            // E18: o prazo enviado é o mesmo, já passado; não volta a ser validado.
            TaskView view = service.update(RUI, TASK,
                    edit(1L, Patch.of("Revisto"), absent(), Patch.of(TODAY.minusDays(2)), absent()));
            assertThat(view.title()).isEqualTo("Revisto");
            assertThat(view.dueDate()).isEqualTo(TODAY.minusDays(2));
        }

        @Test
        void newDueDateCannotBeInThePast() {
            ApiException error = refused(() -> service.update(RUI, TASK,
                    edit(1L, absent(), absent(), Patch.of(TODAY.minusDays(1)), absent())));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("dueDate");
        }

        @Test
        void newDueDateFromTodayOnIsAccepted() {
            TaskView view = service.update(RUI, TASK, edit(1L, absent(), absent(), Patch.of(TODAY.plusDays(3)), absent()));
            assertThat(view.dueDate()).isEqualTo(TODAY.plusDays(3));
            assertThat(view.overdue()).isFalse();
        }

        @Test
        void dueDateCanAlwaysBeRemoved() {
            TaskView view = service.update(RUI, TASK, edit(1L, absent(), absent(), Patch.of(null), absent()));
            assertThat(view.dueDate()).isNull();
            assertThat(view.overdue()).isFalse();
        }

        @Test
        void priorityCannotBeRemoved() {
            ApiException error = refused(() -> service.update(RUI, TASK, edit(1L, absent(), absent(), absent(), Patch.of(null))));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("priority", "A prioridade é obrigatória."));
        }

        @Test
        void descriptionIsValidatedAndCanBeCleared() {
            ApiException error = refused(() -> service.update(RUI, TASK,
                    edit(1L, absent(), Patch.of("d".repeat(2001)), absent(), absent())));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("description");
            assertThat(service.update(RUI, TASK, edit(1L, absent(), Patch.of("  "), absent(), absent())).description())
                    .isNull();
        }

        @Test
        void fieldsThatAreNotSentKeepTheirValue() {
            TaskView view = service.update(RUI, TASK, edit(1L, absent(), absent(), absent(), Patch.of(TaskPriority.LOW)));
            assertThat(view.title()).isEqualTo("Inventário");
            assertThat(view.dueDate()).isEqualTo(TODAY.minusDays(2));
            assertThat(view.priority()).isEqualTo(TaskPriority.LOW);
        }

        @Test
        void tasksOfClosedProjectsCannotBeEdited() {
            givenAccess(RUI, true, true, ProjectStatus.ARCHIVED);
            ApiException error = refused(() -> service.update(RUI, TASK, edit(1L, Patch.of("Novo"), absent(), absent(), absent())));
            assertError(error, HttpStatus.CONFLICT, ProblemCode.PROJECT_CLOSED,
                    "O projeto está concluído ou arquivado e não pode ser alterado.");
        }

        @Test
        void oldVersionIsRefused() {
            ApiException error = refused(() -> service.update(RUI, TASK, edit(0L, Patch.of("Novo"), absent(), absent(), absent())));
            assertError(error, HttpStatus.CONFLICT, ProblemCode.VERSION_CONFLICT,
                    "Esta tarefa foi alterada por outra pessoa. Abra-a de novo e repita a alteração.");
        }

        @Test
        void simultaneousChangeDetectedWhenSavingIsAVersionConflict() {
            when(repository.saveAndFlush(any(Task.class))).thenThrow(new OptimisticLockingFailureException("versão"));
            ApiException error = refused(() -> service.update(RUI, TASK, edit(1L, Patch.of("Novo"), absent(), absent(), absent())));
            assertThat(error.getCode()).isEqualTo(ProblemCode.VERSION_CONFLICT);
        }
    }

    @Test
    void editCommandWithOnlyFieldsThatAreNotEditableStillChangesSomething() {
        // Assim o pedido chega à validação e devolve o erro do campo, e não "nada a alterar".
        assertThat(new UpdateTaskCommand(1L, absent(), absent(), absent(), absent(), Set.of("status")).changesSomething())
                .isTrue();
        assertThat(new UpdateTaskCommand(1L, absent(), absent(), absent(), absent(), Set.of()).changesSomething())
                .isFalse();
    }

    // ───── Mudar o estado (US07, RN15) ─────

    @Nested
    class ChangingTheStatus {

        @BeforeEach
        void openTask() {
            givenTask(task(TaskStatus.TODO, null, 1));
        }

        static Stream<Arguments> whoChangesTheStatus() {
            return Stream.of(
                    arguments("gestor do projeto ou Administrador", true, true, false, true),
                    arguments("responsável, qualquer que seja o papel", false, true, true, true),
                    arguments("Membro que não é responsável", false, true, false, false),
                    arguments("Observador (vê, não é responsável)", false, true, false, false),
                    arguments("responsável num projeto que já não vê", false, false, true, false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("whoChangesTheStatus")
        void permissionToChangeTheStatus(String who, boolean manageable, boolean visible, boolean assignee,
                boolean allowed) {
            givenAccess(MARTA, visible, manageable, ProjectStatus.ACTIVE);
            when(queries.isAssignee(TASK, MARTA.id())).thenReturn(assignee);
            if (allowed) {
                assertThat(service.changeStatus(MARTA, TASK, TaskStatus.DONE, 1L).status()).isEqualTo(TaskStatus.DONE);
            } else {
                assertError(refused(() -> service.changeStatus(MARTA, TASK, TaskStatus.DONE, 1L)),
                        HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, "Não tem permissão para esta operação.");
            }
        }

        @Test
        void statusAndVersionAreRequired() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            ApiException error = refused(() -> service.changeStatus(RUI, TASK, null, null));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("status", "version");
        }

        @Test
        void tasksOfClosedProjectsKeepTheirStatus() {
            givenAccess(RUI, true, true, ProjectStatus.COMPLETED);
            assertThat(refused(() -> service.changeStatus(RUI, TASK, TaskStatus.DONE, 1L)).getCode())
                    .isEqualTo(ProblemCode.PROJECT_CLOSED);
        }

        @Test
        void oldVersionIsRefused() {
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(refused(() -> service.changeStatus(RUI, TASK, TaskStatus.DONE, 0L)).getCode())
                    .isEqualTo(ProblemCode.VERSION_CONFLICT);
        }

        @Test
        void anyChangeBetweenTheThreeStatesIsAllowed() {
            // RN12: também voltar atrás.
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            assertThat(service.changeStatus(RUI, TASK, TaskStatus.DONE, 1L).status()).isEqualTo(TaskStatus.DONE);
            assertThat(service.changeStatus(RUI, TASK, TaskStatus.IN_PROGRESS, 1L).status())
                    .isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    // ───── Apagar (US16, RN19) ─────

    @Nested
    class Deleting {

        @BeforeEach
        void openTask() {
            givenTask(task(TaskStatus.TODO, null, 1));
        }

        @Test
        void onlyWhoManagesTheProjectDeletes() {
            givenAccess(MARTA, true, false, ProjectStatus.ACTIVE);
            assertThat(refused(() -> service.delete(MARTA, TASK)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void tasksOfClosedProjectsAreNotDeleted() {
            givenAccess(RUI, true, true, ProjectStatus.ARCHIVED);
            assertThat(refused(() -> service.delete(RUI, TASK)).getCode()).isEqualTo(ProblemCode.PROJECT_CLOSED);
        }

        @Test
        void deletingMarksTheTaskWithTheOrganizationTime() {
            // RN32: a tarefa fica guardada, marcada como eliminada.
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            service.delete(RUI, TASK);
            ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
            verify(repository).saveAndFlush(saved.capture());
            assertThat(ReflectionTestUtils.getField(saved.getValue(), "deletedAt")).isEqualTo(clock.now());
        }
    }

    // ───── Responsáveis (US06, RN14) ─────

    @Nested
    class Assignees {

        private static final long PERSON = 6L;

        @BeforeEach
        void openTask() {
            givenTask(task(TaskStatus.TODO, null, 1));
            givenAccess(RUI, true, true, ProjectStatus.ACTIVE);
            when(queries.findRow(TASK)).thenReturn(row(TASK, TaskStatus.TODO, null));
        }

        @Test
        void unknownAccountIsNotFound() {
            assertError(refused(() -> service.addAssignee(RUI, TASK, PERSON)),
                    HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "A conta não existe.");
        }

        @Test
        void onlyWhoManagesTheProjectAssigns() {
            givenPerson(Role.MEMBER, AccountStatus.ACTIVE, true);
            givenAccess(MARTA, true, false, ProjectStatus.ACTIVE);
            assertThat(refused(() -> service.addAssignee(MARTA, TASK, PERSON)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void tasksOfClosedProjectsKeepTheirAssignees() {
            givenPerson(Role.MEMBER, AccountStatus.ACTIVE, true);
            givenAccess(RUI, true, true, ProjectStatus.COMPLETED);
            assertThat(refused(() -> service.addAssignee(RUI, TASK, PERSON)).getCode()).isEqualTo(ProblemCode.PROJECT_CLOSED);
        }

        static Stream<Arguments> ineligiblePeople() {
            return Stream.of(
                    arguments("conta desativada", Role.MEMBER, AccountStatus.DEACTIVATED, true),
                    arguments("Observador", Role.OBSERVER, AccountStatus.ACTIVE, true),
                    arguments("Administrador", Role.ADMINISTRATOR, AccountStatus.ACTIVE, true),
                    arguments("fora da equipa do projeto", Role.MEMBER, AccountStatus.ACTIVE, false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ineligiblePeople")
        void onlyActiveManagersAndMembersOfTheTeamCanBeAssignees(String who, Role role, AccountStatus status,
                boolean inTeam) {
            givenPerson(role, status, inTeam);
            assertError(refused(() -> service.addAssignee(RUI, TASK, PERSON)), HttpStatus.CONFLICT,
                    ProblemCode.INVALID_ASSIGNEE,
                    "O responsável tem de ter conta ativa, pertencer à equipa do projeto e ter papel Gestor ou Membro.");
            verify(queries, never()).addAssignee(anyLong(), anyLong());
        }

        @Test
        void managerOfTheTeamCanBeAnAssignee() {
            givenPerson(Role.MANAGER, AccountStatus.ACTIVE, true);
            service.addAssignee(RUI, TASK, PERSON);
            verify(queries).addAssignee(TASK, PERSON);
        }

        @Test
        void samePersonIsNotAssignedTwice() {
            givenPerson(Role.MEMBER, AccountStatus.ACTIVE, true);
            when(queries.isAssignee(TASK, PERSON)).thenReturn(true);
            assertThat(refused(() -> service.addAssignee(RUI, TASK, PERSON)).getCode()).isEqualTo(ProblemCode.DUPLICATE_ASSIGNEE);
        }

        @Test
        void simultaneousAssignmentDetectedByTheDatabaseIsADuplicate() {
            givenPerson(Role.MEMBER, AccountStatus.ACTIVE, true);
            doThrow(new DataIntegrityViolationException("ux_task_assignees_active")).when(queries).addAssignee(TASK, PERSON);
            assertThat(refused(() -> service.addAssignee(RUI, TASK, PERSON)).getCode()).isEqualTo(ProblemCode.DUPLICATE_ASSIGNEE);
            verify(queries, never()).incrementVersion(anyLong());
        }

        @Test
        void assigningIncreasesTheVersion() {
            givenPerson(Role.MEMBER, AccountStatus.ACTIVE, true);
            service.addAssignee(RUI, TASK, PERSON);
            verify(queries).incrementVersion(TASK);
        }

        @Test
        void permissionToRemoveIsCheckedBeforeKnowingWhoIsAnAssignee() {
            // Regressão de F1 (quality-security-review.md).
            givenAccess(MARTA, false, false, ProjectStatus.ACTIVE);
            assertThat(refused(() -> service.removeAssignee(MARTA, TASK, PERSON)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(queries, never()).isAssignee(anyLong(), anyLong());
        }

        @Test
        void removingSomeoneWhoIsNotAnAssigneeIsNotFound() {
            assertError(refused(() -> service.removeAssignee(RUI, TASK, PERSON)), HttpStatus.NOT_FOUND,
                    ProblemCode.NOT_FOUND, "Esta pessoa não é responsável pela tarefa.");
        }

        @Test
        void removingMarksTheAssociationAndIncreasesTheVersion() {
            when(queries.isAssignee(TASK, PERSON)).thenReturn(true);
            service.removeAssignee(RUI, TASK, PERSON);
            verify(queries).removeAssignee(TASK, PERSON, clock.now());
            verify(queries).incrementVersion(TASK);
        }

        @Test
        void removingFromAClosedProjectIsRefused() {
            when(queries.isAssignee(TASK, PERSON)).thenReturn(true);
            givenAccess(RUI, true, true, ProjectStatus.ARCHIVED);
            assertThat(refused(() -> service.removeAssignee(RUI, TASK, PERSON)).getCode()).isEqualTo(ProblemCode.PROJECT_CLOSED);
        }

        private void givenPerson(Role role, AccountStatus status, boolean inTeam) {
            when(accounts.findById(PERSON)).thenReturn(Optional.of(
                    new AccountView(PERSON, "Pessoa", "pessoa@empresa.example", role, status, false)));
            when(teams.belongsTo(TEAM, PERSON)).thenReturn(inTeam);
        }
    }

    // ───── Dados e verificações comuns ─────

    private void givenAccess(CurrentUser user, boolean visible, boolean manageable, ProjectStatus status) {
        when(projects.access(user, PROJECT))
                .thenReturn(Optional.of(new ProjectAccess(PROJECT, TEAM, status, visible, manageable)));
    }

    private void givenTask(Task task) {
        when(repository.findActiveById(TASK)).thenReturn(Optional.of(task));
    }

    private static Task task(TaskStatus status, LocalDate dueDate, long version) {
        Task task = Task.create(PROJECT, "Inventário", null, dueDate, TaskPriority.MEDIUM);
        task.changeStatus(status);
        ReflectionTestUtils.setField(task, "id", TASK);
        ReflectionTestUtils.setField(task, "version", version);
        return task;
    }

    private static TaskRow row(long id, TaskStatus status, LocalDate dueDate) {
        return new TaskRow(id, PROJECT, "Tarefa " + id, null, dueDate, TaskPriority.MEDIUM, status, 0);
    }

    private static TaskRow rowInProject(long id, long project) {
        return new TaskRow(id, project, "Tarefa " + id, null, null, TaskPriority.MEDIUM, TaskStatus.TODO, 0);
    }

    private static AccountView account(long id, String name) {
        return new AccountView(id, name, "p" + id + "@empresa.example", Role.MEMBER, AccountStatus.ACTIVE, false);
    }

    private static CreateTaskCommand command(String title) {
        return new CreateTaskCommand(title, null, null, null);
    }

    private static UpdateTaskCommand edit(Long version, Patch<String> title, Patch<String> description,
            Patch<LocalDate> dueDate, Patch<TaskPriority> priority) {
        return new UpdateTaskCommand(version, title, description, dueDate, priority, Set.of());
    }

    private static <T> Patch<T> absent() {
        return Patch.absent();
    }

    private static CurrentUser user(long id, String name, Role role) {
        return new CurrentUser(id, name, name.toLowerCase() + "@empresa.example", role,
                List.of(new Reference(TEAM, "Logística Norte")), false);
    }

    private static ApiException refused(Executable call) {
        return assertThrows(ApiException.class, call);
    }

    private static void assertError(ApiException error, HttpStatus status, ProblemCode code, String detail) {
        assertThat(error.getStatus()).isEqualTo(status);
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getMessage()).isEqualTo(detail);
    }
}
