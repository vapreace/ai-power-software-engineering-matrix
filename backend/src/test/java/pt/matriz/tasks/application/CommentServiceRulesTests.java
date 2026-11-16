package pt.matriz.tasks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.ApiFieldError;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.OrganizationClock;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.projects.ProjectAccess;
import pt.matriz.projects.ProjectStatus;
import pt.matriz.projects.Projects;
import pt.matriz.tasks.CommentView;
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.persistence.CommentQueries;
import pt.matriz.tasks.persistence.CommentQueries.CommentRow;
import pt.matriz.tasks.persistence.Task;
import pt.matriz.tasks.persistence.TaskRepository;

/**
 * Testes unitários das regras de {@link CommentService} (passo 2b, DA37): quem lê e quem comenta
 * (RN30, requirements.md secção 3.1), texto obrigatório com 1 a 2000 caracteres, projeto fechado
 * (RN09), autor e hora vindos do backend (E43, RNF06).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceRulesTests {

    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private static final long PROJECT = 1L;
    private static final long TEAM = 10L;
    private static final long OTHER_TEAM = 20L;
    private static final long TASK = 42L;

    @Mock
    private TaskRepository tasks;
    @Mock
    private CommentQueries comments;
    @Mock
    private Projects projects;
    @Mock
    private Accounts accounts;

    private OrganizationClock clock;
    private CommentService service;

    @BeforeEach
    void setUp() {
        clock = new OrganizationClock(Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE));
        service = new CommentService(tasks, comments, projects, accounts, clock);
        Task task = Task.create(PROJECT, "Inventário", null, null, TaskPriority.MEDIUM);
        ReflectionTestUtils.setField(task, "id", TASK);
        when(tasks.findActiveById(TASK)).thenReturn(Optional.of(task));
        when(comments.insert(anyLong(), anyLong(), anyString(), any())).thenReturn(7L);
    }

    @Test
    void commentsOfAnUnknownTaskAreNotFound() {
        when(tasks.findActiveById(TASK)).thenReturn(Optional.empty());
        ApiException error = refused(() -> service.list(user(Role.MEMBER, TEAM), TASK));
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getMessage()).isEqualTo("A tarefa não existe.");
    }

    @Test
    void taskWhoseProjectNoLongerExistsIsNotFound() {
        assertThat(refused(() -> service.list(user(Role.MEMBER, TEAM), TASK)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void commentsOfAProjectThatIsNotVisibleAreRefused() {
        CurrentUser joao = user(Role.MEMBER, OTHER_TEAM);
        givenAccess(joao, false, ProjectStatus.ACTIVE);
        ApiException error = refused(() -> service.list(joao, TASK));
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.getMessage()).isEqualTo("Não tem acesso a esta página.");
    }

    @Test
    void commentsAreListedInTheGivenOrderWithTheAuthorNames() {
        CurrentUser carlos = user(Role.OBSERVER, null);
        givenAccess(carlos, true, ProjectStatus.ACTIVE);
        OffsetDateTime earlier = clock.now().minusHours(1);
        when(comments.list(TASK)).thenReturn(List.of(
                new CommentRow(1, 5L, "Primeiro", earlier), new CommentRow(2, 2L, "Segundo", clock.now())));
        when(accounts.findAllById(List.of(5L, 2L))).thenReturn(List.of(account(2, "Rui"), account(5, "Marta")));

        List<CommentView> list = service.list(carlos, TASK);

        assertThat(list).extracting(CommentView::content).containsExactly("Primeiro", "Segundo");
        assertThat(list).extracting(view -> view.author().name()).containsExactly("Marta", "Rui");
    }

    static Stream<Arguments> whoComments() {
        return Stream.of(
                arguments("Administrador, sem equipa", Role.ADMINISTRATOR, null, true),
                arguments("Gestor da equipa do projeto", Role.MANAGER, TEAM, true),
                arguments("Membro da equipa do projeto", Role.MEMBER, TEAM, true),
                arguments("Gestor de outra equipa", Role.MANAGER, OTHER_TEAM, false),
                arguments("Membro de outra equipa", Role.MEMBER, OTHER_TEAM, false),
                arguments("Observador", Role.OBSERVER, null, false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("whoComments")
    void permissionToComment(String who, Role role, Long team, boolean allowed) {
        CurrentUser user = user(role, team);
        givenAccess(user, true, ProjectStatus.ACTIVE);
        if (allowed) {
            assertThat(service.create(user, TASK, "Visto").id()).isEqualTo(7L);
        } else {
            ApiException error = refused(() -> service.create(user, TASK, "Visto"));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(error.getMessage()).isEqualTo("Não tem permissão para esta operação.");
            verify(comments, never()).insert(anyLong(), anyLong(), anyString(), any());
        }
    }

    @ParameterizedTest(name = "texto inválido: [{0}]")
    @ValueSource(strings = {"", "   "})
    void emptyTextIsRefused(String text) {
        CurrentUser marta = user(Role.MEMBER, TEAM);
        givenAccess(marta, true, ProjectStatus.ACTIVE);
        ApiException error = refused(() -> service.create(marta, TASK, text));
        assertThat(error.getErrors()).containsExactly(
                new ApiFieldError("content", "O comentário é obrigatório e tem no máximo 2000 caracteres."));
    }

    @Test
    void missingTextIsRefused() {
        CurrentUser marta = user(Role.MEMBER, TEAM);
        givenAccess(marta, true, ProjectStatus.ACTIVE);
        assertThat(refused(() -> service.create(marta, TASK, null)).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void textHasAtMost2000Characters() {
        CurrentUser marta = user(Role.MEMBER, TEAM);
        givenAccess(marta, true, ProjectStatus.ACTIVE);
        assertThat(service.create(marta, TASK, "c".repeat(2000)).content()).hasSize(2000);
        assertThat(refused(() -> service.create(marta, TASK, "c".repeat(2001))).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void closedProjectsDoNotAcceptCommentsAndValidationComesFirst() {
        CurrentUser marta = user(Role.MEMBER, TEAM);
        givenAccess(marta, true, ProjectStatus.ARCHIVED);
        assertThat(refused(() -> service.create(marta, TASK, " ")).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiException error = refused(() -> service.create(marta, TASK, "Visto"));
        assertThat(error.getCode()).isEqualTo(ProblemCode.PROJECT_CLOSED);
    }

    @Test
    void authorAndTimeComeFromTheBackendAndTheTextIsTrimmed() {
        CurrentUser marta = user(Role.MEMBER, TEAM);
        givenAccess(marta, true, ProjectStatus.ACTIVE);

        CommentView view = service.create(marta, TASK, "  Já pedi orçamentos.  ");

        verify(comments).insert(TASK, marta.id(), "Já pedi orçamentos.", clock.now());
        assertThat(view.author()).isEqualTo(new Reference(marta.id(), marta.name()));
        assertThat(view.createdAt()).isEqualTo(clock.now());
        assertThat(view.content()).isEqualTo("Já pedi orçamentos.");
    }

    private void givenAccess(CurrentUser user, boolean visible, ProjectStatus status) {
        when(projects.access(user, PROJECT))
                .thenReturn(Optional.of(new ProjectAccess(PROJECT, TEAM, status, visible, false)));
    }

    private static CurrentUser user(Role role, Long team) {
        List<Reference> teams = team == null ? List.of() : List.of(new Reference(team, "Equipa " + team));
        return new CurrentUser(5L, "Marta", "marta@empresa.example", role, teams, false);
    }

    private static AccountView account(long id, String name) {
        return new AccountView(id, name, "p" + id + "@empresa.example", Role.MEMBER, AccountStatus.ACTIVE, false);
    }

    private static ApiException refused(Executable call) {
        return assertThrows(ApiException.class, call);
    }
}
