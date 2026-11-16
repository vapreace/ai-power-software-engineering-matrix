package pt.matriz.tasks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

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
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
import pt.matriz.tasks.TaskView;
import pt.matriz.tasks.persistence.Task;
import pt.matriz.tasks.persistence.TaskQueries;
import pt.matriz.tasks.persistence.TaskRepository;
import pt.matriz.teams.Teams;

/**
 * Testes unitários da regra do título da tarefa (RN11, DA11, LIM13), sem base de dados nem Spring
 * (DA37). O {@link TaskService} é real; a persistência e os outros módulos são mocks.
 * Cenários CT04 a CT08 de docs/test-plan.md. Os resultados esperados vêm da RN11 e do contrato,
 * não da implementação: 1 a 200 caracteres depois de retirar os espaços nas pontas e de
 * normalizar em NFC, cada código Unicode conta um.
 */
@ExtendWith(MockitoExtension.class)
class TaskTitleRulesTests {

    /** Mensagem do erro de validação do título, a mesma de TaskApiTests.invalidTitlesAreRefused. */
    private static final String TITLE_INVALID = "O título é obrigatório e tem no máximo 200 caracteres.";

    private static final long PROJECT_ID = 1L;
    private static final long TEAM_ID = 10L;
    private static final long TASK_ID = 42L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");

    /** O Rui, gestor do projeto (pode criar e editar tarefas, requirements.md secção 3.1). */
    private static final CurrentUser RUI = new CurrentUser(2L, "Rui", "rui@empresa.example", Role.MANAGER,
            List.of(new Reference(TEAM_ID, "Logística Norte")), false);

    /** Um emoji simples: um código Unicode. */
    private static final String EMOJI = "🙂";
    /** Bandeira de Portugal: dois códigos Unicode (LIM13). */
    private static final String FLAG = "🇵🇹";
    /** "e" seguido do acento agudo combinado (U+0301): fica "é" depois de normalizar em NFC (DA11). */
    private static final String E_WITH_COMBINING_ACUTE = "é";

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

    private TaskService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE);
        service = new TaskService(repository, queries, projects, accounts, teams, new OrganizationClock(fixed));
        when(projects.access(RUI, PROJECT_ID))
                .thenReturn(Optional.of(new ProjectAccess(PROJECT_ID, TEAM_ID, ProjectStatus.ACTIVE, true, true)));
    }

    static Stream<Arguments> validTitles() {
        return Stream.of(
                arguments("CT07: um carácter", "a", "a"),
                arguments("CT07: 200 caracteres", "a".repeat(200), "a".repeat(200)),
                arguments("CT07: 200 letras acentuadas", "é".repeat(200), "é".repeat(200)),
                arguments("CT07: 200 acentos combinados, normalizados em NFC",
                        E_WITH_COMBINING_ACUTE.repeat(200), "é".repeat(200)),
                arguments("CT07: 200 emojis simples", EMOJI.repeat(200), EMOJI.repeat(200)),
                arguments("CT07: 200 caracteres com espaços nas pontas",
                        "  " + "a".repeat(200) + "  ", "a".repeat(200)),
                arguments("CT04: título aparado", "  Rever contrato  ", "Rever contrato"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTitles")
    void validTitleCreatesTheTask(String scenario, String title, String expectedTitle) {
        // Arrange
        when(repository.saveAndFlush(any(Task.class))).thenAnswer(call -> withId(call.getArgument(0)));
        CreateTaskCommand command = new CreateTaskCommand(title, null, null, null);

        // Act
        TaskView created = service.create(RUI, PROJECT_ID, command);

        // Assert
        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo(expectedTitle);
        assertThat(created.title()).isEqualTo(expectedTitle);
        assertThat(created.status()).isEqualTo(TaskStatus.DONE);
        assertThat(created.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    static Stream<Arguments> invalidTitles() {
        return Stream.of(
                arguments("CT05: em falta", null),
                arguments("CT05: vazio", ""),
                arguments("CT05: só espaços", "   "),
                arguments("CT06: 201 caracteres", "a".repeat(201)),
                arguments("CT06: 201 letras acentuadas", "é".repeat(201)),
                arguments("CT06: 201 caracteres depois de retirar os espaços", " " + "a".repeat(201) + " "),
                arguments("CT06: 101 bandeiras, 202 códigos Unicode", FLAG.repeat(101)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTitles")
    void invalidTitleIsRefusedAndNothingIsSaved(String scenario, String title) {
        // Arrange
        CreateTaskCommand command = new CreateTaskCommand(title, null, null, null);

        // Act and assert
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> service.create(RUI, PROJECT_ID, command))
                .satisfies(TaskTitleRulesTests::assertTitleRefused);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidTitleIsAlsoRefusedWhenEditing() {
        // Arrange (CT08)
        Task task = withId(Task.create(PROJECT_ID, "Inventário", null, null, TaskPriority.MEDIUM));
        when(repository.findActiveById(TASK_ID)).thenReturn(Optional.of(task));
        UpdateTaskCommand command = new UpdateTaskCommand(0L, Patch.of("   "), Patch.absent(), Patch.absent(),
                Patch.absent(), Set.of());

        // Act and assert
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> service.update(RUI, TASK_ID, command))
                .satisfies(TaskTitleRulesTests::assertTitleRefused);
        assertThat(task.getTitle()).isEqualTo("Inventário");
        verify(repository, never()).saveAndFlush(any());
    }

    /** 400 VALIDATION_ERROR com o erro no campo title (DA16, DA17). */
    private static void assertTitleRefused(ApiException error) {
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getCode()).isEqualTo(ProblemCode.VALIDATION_ERROR);
        assertThat(error.getErrors()).containsExactly(new ApiFieldError("title", TITLE_INVALID));
    }

    /** A base de dados daria o identificador ao gravar; aqui é dado à mão. */
    private static Task withId(Task task) {
        ReflectionTestUtils.setField(task, "id", TASK_ID);
        return task;
    }
}
