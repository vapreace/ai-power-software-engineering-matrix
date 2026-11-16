package pt.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
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
import pt.matriz.projects.persistence.Project;
import pt.matriz.projects.persistence.ProjectQueries;
import pt.matriz.projects.persistence.ProjectRepository;
import pt.matriz.projects.persistence.ProjectRow;
import pt.matriz.teams.Teams;

/**
 * Testes unitários das regras de {@link Projects} (passo 2b, DA37): visibilidade e permissões
 * (requirements.md secção 3.1, RN05, RN22, RN26), validações (RN06, RN29, RN34), gestor (RN21, RN27),
 * estados (RN07, RN09), eliminação (RN31, RN32) e versão (DA05). O serviço é real; a persistência e
 * os outros módulos são mocks. A data atual é 10/11/2026 (Relógio fixo).
 * <p>
 * Equipa 10 (Logística Norte): Rui (gere o projeto 1), Sofia (Gestor, não gere) e Marta (Membro).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectsRulesTests {

    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final long PROJECT = 1L;
    private static final long TEAM = 10L;
    private static final long OTHER_TEAM = 20L;

    private static final CurrentUser ANA = user(1L, "Ana", Role.ADMINISTRATOR);
    private static final CurrentUser RUI = user(2L, "Rui", Role.MANAGER, TEAM);
    private static final CurrentUser SOFIA = user(3L, "Sofia", Role.MANAGER, TEAM);
    private static final CurrentUser TIAGO = user(4L, "Tiago", Role.MANAGER, OTHER_TEAM);
    private static final CurrentUser MARTA = user(5L, "Marta", Role.MEMBER, TEAM);
    private static final CurrentUser PEDRO_WITHOUT_TEAM = user(6L, "Pedro", Role.MEMBER);
    private static final CurrentUser CARLOS = user(8L, "Carlos", Role.OBSERVER);
    /** O Rui depois de sair da equipa: continua gestor no projeto até RN22 o limpar. */
    private static final CurrentUser RUI_OUTSIDE_TEAM = user(2L, "Rui", Role.MANAGER, OTHER_TEAM);

    @Mock
    private ProjectRepository repository;
    @Mock
    private ProjectQueries queries;
    @Mock
    private Teams teams;
    @Mock
    private Accounts accounts;
    @Mock
    private ApplicationEventPublisher events;

    private OrganizationClock clock;
    private Projects projects;

    @BeforeEach
    void setUp() {
        clock = new OrganizationClock(Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE));
        projects = new Projects(repository, queries, teams, accounts, clock, events);
        when(repository.saveAndFlush(any(Project.class))).thenAnswer(call -> {
            Project saved = call.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", PROJECT);
            }
            return saved;
        });
        when(teams.findTeam(TEAM)).thenReturn(Optional.of(new Reference(TEAM, "Logística Norte")));
        when(teams.findTeam(OTHER_TEAM)).thenReturn(Optional.of(new Reference(OTHER_TEAM, "Compras")));
        when(teams.hasActiveManager(anyLong())).thenReturn(true);
        when(teams.teamNames(any())).thenReturn(Map.of(TEAM, "Logística Norte", OTHER_TEAM, "Compras"));
    }

    // ───── Pesquisar (US12, US22, RN25, RN34) ─────

    @Nested
    class Searching {

        @Test
        void queryHasAtMost100Characters() {
            projects.search(ANA, Set.of(), "a".repeat(100));
            ApiException error = refused(() -> projects.search(ANA, Set.of(), "a".repeat(101)));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("q", "A pesquisa tem no máximo 100 caracteres."));
        }

        @Test
        void withoutStatusesShowsActiveAndCompletedAndIgnoresEmptyValues() {
            projects.search(ANA, null, null);
            projects.search(ANA, new HashSet<>(Arrays.asList((ProjectStatus) null)), "   ");
            verify(queries, org.mockito.Mockito.times(2))
                    .search(EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.COMPLETED), null, null);
        }

        @Test
        void chosenStatusesAndTrimmedTextAreUsed() {
            projects.search(ANA, Set.of(ProjectStatus.ARCHIVED), "  armazém ");
            verify(queries).search(Set.of(ProjectStatus.ARCHIVED), null, "armazém");
        }

        @ParameterizedTest(name = "{0} vê todos os projetos")
        @EnumSource(value = Role.class, names = {"ADMINISTRATOR", "OBSERVER"})
        void administratorAndObserverSeeAllProjects(Role role) {
            projects.search(user(9L, "X", role), Set.of(), null);
            verify(queries).search(any(), isNull(), isNull());
        }

        @Test
        void managersAndMembersSeeOnlyTheProjectsOfTheirTeams() {
            projects.search(user(5L, "Marta", Role.MEMBER, TEAM, OTHER_TEAM), Set.of(), null);
            verify(queries).search(any(), eq(List.of(TEAM, OTHER_TEAM)), isNull());
        }

        @Test
        void memberWithoutTeamSeesNoProjects() {
            // RN26, E26.
            assertThat(projects.search(PEDRO_WITHOUT_TEAM, Set.of(), null)).isEmpty();
            verify(queries, never()).search(any(), any(), any());
        }

        @Test
        void resultHasTheTeamAndManagerNamesAndProjectsWithoutManager() {
            when(queries.search(any(), any(), any())).thenReturn(List.of(
                    row(1L, TEAM, RUI.id()), row(2L, OTHER_TEAM, null)));
            when(accounts.findAllById(List.of(RUI.id()))).thenReturn(List.of(account(RUI.id(), "Rui", Role.MANAGER)));

            List<ProjectView> found = projects.search(ANA, Set.of(), null);

            assertThat(found.get(0).team()).isEqualTo(new Reference(TEAM, "Logística Norte"));
            assertThat(found.get(0).manager()).isEqualTo(new Reference(RUI.id(), "Rui"));
            assertThat(found.get(1).manager()).isNull();
        }
    }

    // ───── Consultar e acesso (US12, US15, SEG02) ─────

    @Nested
    class ReadingAndAccess {

        @Test
        void deletedOrUnknownProjectIsNotFound() {
            ApiException error = refused(() -> projects.get(ANA, PROJECT));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.getMessage()).isEqualTo("O projeto não existe.");
            assertThat(projects.access(ANA, PROJECT)).isEmpty();
        }

        @Test
        void projectOfAnotherTeamIsRefused() {
            givenProject(project(ProjectStatus.ACTIVE, RUI.id(), 0));
            ApiException error = refused(() -> projects.get(TIAGO, PROJECT));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(error.getMessage()).isEqualTo("Não tem acesso a esta página.");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("pt.matriz.projects.ProjectsRulesTests#whoSeesAndManages")
        void visibilityAndManagement(String who, CurrentUser user, Long managerId, boolean visible, boolean manageable) {
            givenProject(project(ProjectStatus.ACTIVE, managerId, 0));
            ProjectAccess access = projects.access(user, PROJECT).orElseThrow();
            assertThat(access.visible()).isEqualTo(visible);
            assertThat(access.manageable()).isEqualTo(manageable);
            assertThat(access.teamId()).isEqualTo(TEAM);
            if (visible) {
                assertThat(projects.get(user, PROJECT).id()).isEqualTo(PROJECT);
            }
        }
    }

    static Stream<Arguments> whoSeesAndManages() {
        return Stream.of(
                arguments("Administrador", ANA, 2L, true, true),
                arguments("gestor do projeto", RUI, 2L, true, true),
                arguments("gestor que saiu da equipa (RN22)", RUI_OUTSIDE_TEAM, 2L, false, false),
                arguments("outro Gestor da equipa", SOFIA, 2L, true, false),
                arguments("Gestor num projeto \"Sem gestor\"", RUI, null, true, false),
                arguments("Administrador num projeto \"Sem gestor\"", ANA, null, true, true),
                arguments("Membro da equipa", MARTA, 2L, true, false),
                arguments("Observador", CARLOS, 2L, true, false),
                arguments("Gestor de outra equipa", TIAGO, 2L, false, false),
                arguments("Membro sem equipa", PEDRO_WITHOUT_TEAM, 2L, false, false));
    }

    // ───── Criar (US04, RN05, RN06, RN21, RN27) ─────

    @Nested
    class Creating {

        @Test
        void unknownTeamIsNotFound() {
            ApiException error = refused(() -> projects.create(ANA, create(99L, 2L)));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.getMessage()).isEqualTo("A equipa não existe.");
        }

        @ParameterizedTest(name = "{0} não cria")
        @MethodSource("pt.matriz.projects.ProjectsRulesTests#whoCannotCreate")
        void onlyAdministratorsAndManagersOfTheTeamCreate(String who, CurrentUser user) {
            ApiException error = refused(() -> projects.create(user, create(TEAM, null)));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void managerWithoutTeamChosenGetsTheValidationError() {
            ApiException error = refused(() -> projects.create(RUI, new CreateProjectCommand(null, "Frota", null, START, null, null)));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("teamId", "A equipa é obrigatória."));
        }

        @Test
        void allInvalidFieldsAreReturnedTogether() {
            ApiException error = refused(() -> projects.create(ANA,
                    new CreateProjectCommand(TEAM, " ", "d".repeat(1001), null, START, null)));
            assertThat(error.getErrors()).extracting(ApiFieldError::field)
                    .containsExactly("name", "description", "startDate", "managerId");
        }

        @Test
        void nameHasAtMost100CharactersAndDescriptionAtMost1000() {
            assertThat(projects.create(RUI, new CreateProjectCommand(TEAM, "n".repeat(100), "d".repeat(1000), START, null, null))
                    .name()).hasSize(100);
            ApiException error = refused(() -> projects.create(RUI,
                    new CreateProjectCommand(TEAM, "n".repeat(101), null, START, null, null)));
            assertThat(error.getErrors()).containsExactly(
                    new ApiFieldError("name", "O nome é obrigatório e tem no máximo 100 caracteres."));
        }

        @Test
        void expectedEndDateCannotBeBeforeTheStartDateButCanBeTheSameDay() {
            assertThat(projects.create(RUI, new CreateProjectCommand(TEAM, "Frota", null, START, START, null))
                    .expectedEndDate()).isEqualTo(START);
            ApiException error = refused(() -> projects.create(RUI,
                    new CreateProjectCommand(TEAM, "Frota", null, START, START.minusDays(1), null)));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("expectedEndDate",
                    "A data de fim prevista não pode ser anterior à data de início."));
        }

        @Test
        void teamWithoutAnActiveManagerCannotHaveNewProjects() {
            when(teams.hasActiveManager(TEAM)).thenReturn(false);
            ApiException error = refused(() -> projects.create(RUI, create(TEAM, null)));
            assertThat(error.getCode()).isEqualTo(ProblemCode.TEAM_WITHOUT_MANAGER);
        }

        @Test
        void managerBecomesTheManagerAndTheIndicatedManagerIsIgnored() {
            ProjectView created = projects.create(RUI, create(TEAM, SOFIA.id()));
            assertThat(created.manager().id()).isEqualTo(RUI.id());
            assertThat(created.status()).isEqualTo(ProjectStatus.ACTIVE);
        }

        @Test
        void administratorCreatesInAnyTeamWithAValidManager() {
            givenAccount(SOFIA.id(), Role.MANAGER, AccountStatus.ACTIVE, true);
            ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);

            projects.create(ANA, create(TEAM, SOFIA.id()));

            verify(repository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getManagerId()).isEqualTo(SOFIA.id());
        }

        static Stream<Arguments> invalidManagers() {
            return Stream.of(
                    arguments("conta que não existe", null, null, true),
                    arguments("Membro", Role.MEMBER, AccountStatus.ACTIVE, true),
                    arguments("conta desativada", Role.MANAGER, AccountStatus.DEACTIVATED, true),
                    arguments("fora da equipa", Role.MANAGER, AccountStatus.ACTIVE, false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidManagers")
        void administratorMustIndicateAValidManager(String which, Role role, AccountStatus status, boolean inTeam) {
            if (role != null) {
                givenAccount(SOFIA.id(), role, status, inTeam);
            }
            ApiException error = refused(() -> projects.create(ANA, create(TEAM, SOFIA.id())));
            assertThat(error.getCode()).isEqualTo(ProblemCode.INVALID_MANAGER);
            verify(repository, never()).saveAndFlush(any());
        }
    }

    static Stream<Arguments> whoCannotCreate() {
        return Stream.of(
                arguments("Membro", MARTA),
                arguments("Observador", CARLOS),
                arguments("Gestor de outra equipa", TIAGO));
    }

    // ───── Editar (US17, RN09) ─────

    @Nested
    class Editing {

        @BeforeEach
        void activeProject() {
            givenProject(project(ProjectStatus.ACTIVE, RUI.id(), 2));
        }

        @Test
        void unknownProjectIsNotFound() {
            when(repository.findActiveById(PROJECT)).thenReturn(Optional.empty());
            assertThat(refused(() -> projects.update(RUI, PROJECT, edit(2L, Patch.of("X"), absent(), absent(), absent())))
                    .getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @ParameterizedTest(name = "{0} não edita")
        @MethodSource("pt.matriz.projects.ProjectsRulesTests#whoCannotManage")
        void onlyTheAdministratorAndTheManagerEdit(String who, CurrentUser user) {
            ApiException error = refused(() -> projects.update(user, PROJECT, edit(2L, Patch.of(""), absent(), absent(), absent())));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(error.getMessage()).isEqualTo("Não tem permissão para esta operação.");
        }

        @Test
        void teamCannotBeChangedAndVersionIsRequired() {
            UpdateProjectCommand command = new UpdateProjectCommand(null, absent(), absent(), absent(), absent(), Set.of("teamId"));
            ApiException error = refused(() -> projects.update(RUI, PROJECT, command));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("teamId", "Este campo não pode ser alterado."),
                    new ApiFieldError("version", "A versão é obrigatória."));
        }

        @Test
        void nameDescriptionAndStartDateAreValidated() {
            ApiException error = refused(() -> projects.update(RUI, PROJECT,
                    edit(2L, Patch.of(" "), Patch.of("d".repeat(1001)), Patch.of(null), absent())));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("name", "description", "startDate");
        }

        @Test
        void newEndDateIsCheckedAgainstTheCurrentStartDate() {
            ApiException error = refused(() -> projects.update(RUI, PROJECT,
                    edit(2L, absent(), absent(), absent(), Patch.of(START.minusDays(1)))));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("expectedEndDate");
        }

        @Test
        void atLeastOneFieldMustChange() {
            ApiException error = refused(() -> projects.update(RUI, PROJECT, edit(2L, absent(), absent(), absent(), absent())));
            assertThat(error.getMessage()).isEqualTo("Indique pelo menos um campo a alterar.");
        }

        @ParameterizedTest(name = "projeto {0}")
        @EnumSource(value = ProjectStatus.class, names = {"COMPLETED", "ARCHIVED"})
        void closedProjectsAreNotEdited(ProjectStatus status) {
            givenProject(project(status, RUI.id(), 2));
            assertThat(refused(() -> projects.update(RUI, PROJECT, edit(2L, Patch.of("Frota"), absent(), absent(), absent())))
                    .getCode()).isEqualTo(ProblemCode.PROJECT_CLOSED);
        }

        @Test
        void oldVersionAndSimultaneousChangeAreVersionConflicts() {
            assertThat(refused(() -> projects.update(RUI, PROJECT, edit(1L, Patch.of("Frota"), absent(), absent(), absent())))
                    .getCode()).isEqualTo(ProblemCode.VERSION_CONFLICT);
            when(repository.saveAndFlush(any(Project.class))).thenThrow(new OptimisticLockingFailureException("versão"));
            assertThat(refused(() -> projects.update(RUI, PROJECT, edit(2L, Patch.of("Frota"), absent(), absent(), absent())))
                    .getCode()).isEqualTo(ProblemCode.VERSION_CONFLICT);
        }

        @Test
        void onlySentFieldsChangeAndTheEndDateCanBeRemoved() {
            ProjectView view = projects.update(RUI, PROJECT, edit(2L, absent(), Patch.of("  "), absent(), Patch.of(null)));
            assertThat(view.name()).isEqualTo("Novo armazém");
            assertThat(view.description()).isNull();
            assertThat(view.startDate()).isEqualTo(START);
            assertThat(view.expectedEndDate()).isNull();
        }

        @Test
        void newStartDateIsAcceptedAndTheEndDateIsCheckedAgainstIt() {
            assertThat(projects.update(RUI, PROJECT, edit(2L, absent(), absent(), Patch.of(START.plusDays(5)), absent()))
                    .startDate()).isEqualTo(START.plusDays(5));
            ApiException error = refused(() -> projects.update(RUI, PROJECT,
                    edit(2L, absent(), absent(), Patch.of(START.plusMonths(3)), absent())));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("expectedEndDate");
        }

        @Test
        void nameCannotBeRemoved() {
            ApiException error = refused(() -> projects.update(RUI, PROJECT, edit(2L, Patch.of(null), absent(), absent(), absent())));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("name");
        }

        @Test
        void administratorEditsAProjectWithoutManager() {
            givenProject(project(ProjectStatus.ACTIVE, null, 2));
            assertThat(projects.update(ANA, PROJECT, edit(2L, Patch.of("Frota"), absent(), absent(), absent())).name())
                    .isEqualTo("Frota");
        }
    }

    static Stream<Arguments> whoCannotManage() {
        return Stream.of(
                arguments("Membro da equipa", MARTA),
                arguments("outro Gestor da equipa", SOFIA),
                arguments("gestor que saiu da equipa", RUI_OUTSIDE_TEAM),
                arguments("Observador", CARLOS),
                arguments("Gestor de outra equipa", TIAGO));
    }

    // ───── Estado (US13, RN07) e gestor (US17, RN21) ─────

    @Nested
    class StatusAndManager {

        @BeforeEach
        void activeProject() {
            givenProject(project(ProjectStatus.ACTIVE, RUI.id(), 2));
        }

        @Test
        void onlyWhoManagesTheProjectChangesTheStatus() {
            assertThat(refused(() -> projects.changeStatus(MARTA, PROJECT, ProjectStatus.COMPLETED, 2L)).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void statusAndVersionAreRequired() {
            ApiException error = refused(() -> projects.changeStatus(RUI, PROJECT, null, null));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("status", "version");
        }

        @Test
        void transitionThatIsNotAllowedIsRefused() {
            ApiException error = refused(() -> projects.changeStatus(RUI, PROJECT, ProjectStatus.ARCHIVED, 2L));
            assertThat(error.getCode()).isEqualTo(ProblemCode.INVALID_TRANSITION);
            assertThat(error.getMessage()).isEqualTo("Esta mudança de estado não é permitida.");
        }

        @Test
        void allowedTransitionChangesTheStatus() {
            assertThat(projects.changeStatus(RUI, PROJECT, ProjectStatus.COMPLETED, 2L).status())
                    .isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        void statusChangeChecksTheVersion() {
            assertThat(refused(() -> projects.changeStatus(RUI, PROJECT, ProjectStatus.COMPLETED, 1L)).getCode())
                    .isEqualTo(ProblemCode.VERSION_CONFLICT);
            when(repository.saveAndFlush(any(Project.class))).thenThrow(new OptimisticLockingFailureException("versão"));
            assertThat(refused(() -> projects.changeStatus(RUI, PROJECT, ProjectStatus.COMPLETED, 2L)).getCode())
                    .isEqualTo(ProblemCode.VERSION_CONFLICT);
        }

        @Test
        void unknownProjectIsNotFoundBeforeAnyOtherCheck() {
            when(repository.findActiveById(PROJECT)).thenReturn(Optional.empty());
            assertThat(refused(() -> projects.changeStatus(MARTA, PROJECT, null, null)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(refused(() -> projects.changeManager(MARTA, PROJECT, null, null)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void onlyTheAdministratorChangesTheManager() {
            assertThat(refused(() -> projects.changeManager(RUI, PROJECT, SOFIA.id(), 2L)).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void managerAndVersionAreRequired() {
            ApiException error = refused(() -> projects.changeManager(ANA, PROJECT, null, null));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("managerId", "version");
        }

        @Test
        void managerChangeChecksTheVersionAndTheNewManager() {
            assertThat(refused(() -> projects.changeManager(ANA, PROJECT, SOFIA.id(), 1L)).getCode())
                    .isEqualTo(ProblemCode.VERSION_CONFLICT);
            givenAccount(SOFIA.id(), Role.MEMBER, AccountStatus.ACTIVE, true);
            assertThat(refused(() -> projects.changeManager(ANA, PROJECT, SOFIA.id(), 2L)).getCode())
                    .isEqualTo(ProblemCode.INVALID_MANAGER);
        }

        @Test
        void managerCanBeChangedAlsoInAnArchivedProject() {
            // Exceção de RN09.
            givenProject(project(ProjectStatus.ARCHIVED, null, 2));
            givenAccount(SOFIA.id(), Role.MANAGER, AccountStatus.ACTIVE, true);
            assertThat(projects.changeManager(ANA, PROJECT, SOFIA.id(), 2L).manager().id()).isEqualTo(SOFIA.id());
        }
    }

    // ───── Eliminar (US20, RN31) e regras automáticas (RN22) ─────

    @Nested
    class DeletingAndAutomaticRules {

        @Test
        void onlyWhoManagesTheProjectDeletes() {
            givenProject(project(ProjectStatus.ACTIVE, RUI.id(), 0));
            assertThat(refused(() -> projects.delete(SOFIA, PROJECT)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(events, never()).publishEvent(any(Object.class));
        }

        @Test
        void deletingInAnyStatusMarksTheProjectAndPublishesTheEvent() {
            Project archived = project(ProjectStatus.ARCHIVED, RUI.id(), 0);
            givenProject(archived);

            projects.delete(RUI, PROJECT);

            assertThat(ReflectionTestUtils.getField(archived, "deletedAt")).isEqualTo(clock.now());
            verify(events).publishEvent(new ProjectDeleted(PROJECT, clock.now()));
        }

        @Test
        void automaticRulesAreDelegatedToTheQueries() {
            when(queries.idsOfTeam(TEAM)).thenReturn(List.of(PROJECT));
            when(queries.activeNames(List.of(PROJECT))).thenReturn(Map.of(PROJECT, "Novo armazém"));

            projects.clearManager(RUI.id(), TEAM);

            verify(queries).clearManager(RUI.id(), TEAM);
            assertThat(projects.projectIdsOfTeam(TEAM)).containsExactly(PROJECT);
            assertThat(projects.activeProjectNames(List.of(PROJECT))).containsEntry(PROJECT, "Novo armazém");
        }
    }

    // ───── Regras puras ─────

    @Nested
    class PureRules {

        static Stream<Arguments> transitions() {
            return Stream.of(
                    arguments(ProjectStatus.ACTIVE, ProjectStatus.ACTIVE, false),
                    arguments(ProjectStatus.ACTIVE, ProjectStatus.COMPLETED, true),
                    arguments(ProjectStatus.ACTIVE, ProjectStatus.ARCHIVED, false),
                    arguments(ProjectStatus.COMPLETED, ProjectStatus.ACTIVE, true),
                    arguments(ProjectStatus.COMPLETED, ProjectStatus.COMPLETED, false),
                    arguments(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED, true),
                    arguments(ProjectStatus.ARCHIVED, ProjectStatus.ACTIVE, false),
                    arguments(ProjectStatus.ARCHIVED, ProjectStatus.COMPLETED, true),
                    arguments(ProjectStatus.ARCHIVED, ProjectStatus.ARCHIVED, false));
        }

        @ParameterizedTest(name = "{0} para {1}: {2}")
        @MethodSource("transitions")
        void allowedTransitionsAreThoseOfTheStateDiagram(ProjectStatus from, ProjectStatus to, boolean allowed) {
            // RN07, requirements.md secção 4.1.
            assertThat(from.canChangeTo(to)).isEqualTo(allowed);
        }

        @Test
        void editCommandWithOnlyFieldsThatAreNotEditableStillChangesSomething() {
            // Assim o pedido chega à validação e devolve o erro do campo, e não "nada a alterar".
            assertThat(new UpdateProjectCommand(1L, absent(), absent(), absent(), absent(), Set.of("teamId"))
                    .changesSomething()).isTrue();
            assertThat(new UpdateProjectCommand(1L, absent(), absent(), Patch.of(START), absent(), Set.of())
                    .changesSomething()).isTrue();
            assertThat(new UpdateProjectCommand(1L, absent(), absent(), absent(), Patch.of(START), Set.of())
                    .changesSomething()).isTrue();
        }
    }

    // ───── Dados e verificações comuns ─────

    private void givenProject(Project project) {
        when(repository.findActiveById(PROJECT)).thenReturn(Optional.of(project));
    }

    private void givenAccount(long id, Role role, AccountStatus status, boolean inTeam) {
        when(accounts.findById(id)).thenReturn(Optional.of(account(id, "Pessoa", role, status)));
        when(teams.belongsTo(TEAM, id)).thenReturn(inTeam);
    }

    private static Project project(ProjectStatus status, Long managerId, long version) {
        Project project = Project.create(TEAM, 0L, "Novo armazém", "Descrição", START, START.plusMonths(2));
        ReflectionTestUtils.setField(project, "id", PROJECT);
        ReflectionTestUtils.setField(project, "managerId", managerId);
        ReflectionTestUtils.setField(project, "version", version);
        project.changeStatus(status);
        return project;
    }

    private static ProjectRow row(long id, long team, Long managerId) {
        return new ProjectRow(id, team, managerId, "Projeto " + id, null, START, null, ProjectStatus.ACTIVE, 0);
    }

    private static CreateProjectCommand create(Long team, Long managerId) {
        return new CreateProjectCommand(team, "Frota", null, START, null, managerId);
    }

    private static UpdateProjectCommand edit(Long version, Patch<String> name, Patch<String> description,
            Patch<LocalDate> startDate, Patch<LocalDate> expectedEndDate) {
        return new UpdateProjectCommand(version, name, description, startDate, expectedEndDate, Set.of());
    }

    private static <T> Patch<T> absent() {
        return Patch.absent();
    }

    private static AccountView account(long id, String name, Role role) {
        return account(id, name, role, AccountStatus.ACTIVE);
    }

    private static AccountView account(long id, String name, Role role, AccountStatus status) {
        return new AccountView(id, name, "p" + id + "@empresa.example", role, status, false);
    }

    private static CurrentUser user(long id, String name, Role role, Long... teams) {
        List<Reference> references = Arrays.stream(teams).map(team -> new Reference(team, "Equipa " + team)).toList();
        return new CurrentUser(id, name, name.toLowerCase() + "@empresa.example", role, references, false);
    }

    private static ApiException refused(Executable call) {
        return assertThrows(ApiException.class, call);
    }
}
