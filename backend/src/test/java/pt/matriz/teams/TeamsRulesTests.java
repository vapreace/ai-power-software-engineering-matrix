package pt.matriz.teams;

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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.OrganizationClock;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.teams.persistence.TeamQueries;

/**
 * Testes unitários das regras de {@link Teams} (passo 2b, DA37): quem vê as equipas e as pessoas
 * (requirements.md secção 3.1), só o Administrador gere (RF05, RF06), nome único (RN03), quem pode
 * pertencer (RN26), Gestor ativo (RN27) e o evento "Pessoa removida da equipa" (RN17, RN22, RN23).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamsRulesTests {

    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private static final long NORTE = 10L;
    private static final long COMPRAS = 20L;
    private static final long PEDRO_ID = 6L;

    private static final CurrentUser ANA = user(1L, Role.ADMINISTRATOR);
    private static final CurrentUser CARLOS = user(8L, Role.OBSERVER);
    private static final CurrentUser MARTA = user(5L, Role.MEMBER, NORTE);
    private static final CurrentUser RUI = user(2L, Role.MANAGER, NORTE);

    @Mock
    private TeamQueries queries;
    @Mock
    private Accounts accounts;
    @Mock
    private ApplicationEventPublisher events;

    private OrganizationClock clock;
    private Teams teams;

    @BeforeEach
    void setUp() {
        clock = new OrganizationClock(Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE));
        teams = new Teams(queries, accounts, clock, events);
        when(queries.findTeam(NORTE)).thenReturn(Optional.of(new Reference(NORTE, "Logística Norte")));
        when(queries.findTeam(COMPRAS)).thenReturn(Optional.of(new Reference(COMPRAS, "Compras")));
        when(queries.insertTeam(any())).thenReturn(30L);
    }

    // ───── Ver equipas e pessoas ─────

    @Nested
    class Seeing {

        @Test
        void administratorAndObserverSeeAllTeamsOthersOnlyTheirOwn() {
            when(queries.allTeams()).thenReturn(List.of(new Reference(COMPRAS, "Compras"), new Reference(NORTE, "Logística Norte")));
            when(queries.activeTeamsOf(MARTA.id())).thenReturn(List.of(new Reference(NORTE, "Logística Norte")));
            assertThat(teams.visibleTeams(ANA)).hasSize(2);
            assertThat(teams.visibleTeams(CARLOS)).hasSize(2);
            assertThat(teams.visibleTeams(MARTA)).extracting(Reference::id).containsExactly(NORTE);
            assertThat(teams.teamsOf(MARTA.id())).extracting(Reference::id).containsExactly(NORTE);
        }

        @Test
        void membersOfAnUnknownTeamAreNotFound() {
            ApiException error = refused(() -> teams.members(ANA, 99L));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.getMessage()).isEqualTo("A equipa não existe.");
        }

        @Test
        void managersAndMembersOnlySeeThePeopleOfTheirTeams() {
            ApiException error = refused(() -> teams.members(RUI, COMPRAS));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(error.getMessage()).isEqualTo("Não tem acesso a esta página.");
        }

        @Test
        void peopleAreOrderedByNameWithoutAccentsOrCase() {
            when(queries.activeMemberIds(NORTE)).thenReturn(List.of(2L, 5L, 6L, 3L));
            when(accounts.findAllById(List.of(2L, 5L, 6L, 3L))).thenReturn(List.of(
                    account(2L, "Rui", Role.MANAGER, AccountStatus.ACTIVE),
                    account(5L, "marta", Role.MEMBER, AccountStatus.ACTIVE),
                    account(6L, "Pedro", Role.MEMBER, AccountStatus.DEACTIVATED),
                    account(3L, "Álvaro", Role.MANAGER, AccountStatus.ACTIVE)));

            List<TeamMember> members = teams.members(MARTA, NORTE);

            assertThat(members).extracting(TeamMember::name).containsExactly("Álvaro", "marta", "Pedro", "Rui");
            assertThat(members.get(2).status()).isEqualTo(AccountStatus.DEACTIVATED);
            assertThat(teams.members(CARLOS, NORTE)).hasSize(4);
        }

        @Test
        void simpleQueriesAreDelegated() {
            when(queries.teamsByIds(List.of(NORTE))).thenReturn(List.of(new Reference(NORTE, "Logística Norte")));
            when(queries.isActiveMember(NORTE, PEDRO_ID)).thenReturn(true);
            assertThat(teams.teamNames(List.of(NORTE))).containsEntry(NORTE, "Logística Norte");
            assertThat(teams.belongsTo(NORTE, PEDRO_ID)).isTrue();
            assertThat(teams.findTeam(NORTE)).contains(new Reference(NORTE, "Logística Norte"));
        }

        static Stream<Arguments> managersOfTheTeam() {
            return Stream.of(
                    arguments("um Gestor ativo", Role.MANAGER, AccountStatus.ACTIVE, true),
                    arguments("só um Gestor desativado", Role.MANAGER, AccountStatus.DEACTIVATED, false),
                    arguments("só Membros", Role.MEMBER, AccountStatus.ACTIVE, false));
        }

        @ParameterizedTest(name = "equipa com {0}: tem Gestor ativo = {3}")
        @MethodSource("managersOfTheTeam")
        void teamHasAnActiveManager(String which, Role role, AccountStatus status, boolean expected) {
            when(queries.activeMemberIds(NORTE)).thenReturn(List.of(4L));
            when(accounts.findAllById(List.of(4L))).thenReturn(List.of(account(4L, "Pessoa", role, status)));
            assertThat(teams.hasActiveManager(NORTE)).isEqualTo(expected);
        }
    }

    // ───── Criar e mudar o nome (US03, RN03) ─────

    @Nested
    class Naming {

        @Test
        void onlyTheAdministratorCreatesAndRenames() {
            assertThat(refused(() -> teams.create(RUI, "Armazém Central")).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(refused(() -> teams.rename(RUI, NORTE, "Norte")).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(queries, never()).insertTeam(any());
        }

        @ParameterizedTest(name = "nome inválido: [{0}]")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void nameIsRequired(String name) {
            ApiException error = refused(() -> teams.create(ANA, name));
            assertThat(error.getErrors()).extracting(e -> e.field()).containsExactly("name");
        }

        @Test
        void nameHasAtMost100Characters() {
            assertThat(teams.create(ANA, "n".repeat(100)).name()).hasSize(100);
            assertThat(refused(() -> teams.create(ANA, "n".repeat(101))).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void newTeamHasTheTrimmedName() {
            assertThat(teams.create(ANA, "  Armazém Central  ")).isEqualTo(new Reference(30L, "Armazém Central"));
            verify(queries).nameExists("Armazém Central", null);
        }

        @Test
        void repeatedNameIsRefusedAlsoWhenTwoRequestsArriveTogether() {
            when(queries.nameExists("Compras", null)).thenReturn(true);
            assertThat(refused(() -> teams.create(ANA, "Compras")).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_NAME);
            when(queries.insertTeam("Vendas")).thenThrow(new DuplicateKeyException("ux_teams_name_lower"));
            assertThat(refused(() -> teams.create(ANA, "Vendas")).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_NAME);
        }

        @Test
        void renamingChecksTheTeamTheNameAndOtherTeams() {
            assertThat(refused(() -> teams.rename(ANA, 99L, "X")).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(refused(() -> teams.rename(ANA, NORTE, " ")).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            when(queries.nameExists("Compras", NORTE)).thenReturn(true);
            assertThat(refused(() -> teams.rename(ANA, NORTE, "Compras")).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_NAME);
            doThrow(new DuplicateKeyException("ux_teams_name_lower")).when(queries).renameTeam(NORTE, "Vendas");
            assertThat(refused(() -> teams.rename(ANA, NORTE, "Vendas")).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_NAME);
        }

        @Test
        void renamingKeepsTheTeamIdentifier() {
            assertThat(teams.rename(ANA, NORTE, "  Norte  ")).isEqualTo(new Reference(NORTE, "Norte"));
            verify(queries).renameTeam(NORTE, "Norte");
        }
    }

    // ───── Pessoas da equipa (US03, RN26, RN17) ─────

    @Nested
    class Membership {

        @Test
        void addingChecksTheTeamTheAccountAndThePermissionInThisOrder() {
            assertThat(refused(() -> teams.addMember(ANA, 99L, PEDRO_ID)).getMessage()).isEqualTo("A equipa não existe.");
            assertThat(refused(() -> teams.addMember(ANA, NORTE, PEDRO_ID)).getMessage()).isEqualTo("A conta não existe.");
            givenAccount(Role.MEMBER, AccountStatus.ACTIVE);
            assertThat(refused(() -> teams.addMember(RUI, NORTE, PEDRO_ID)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        static Stream<Arguments> whoCannotBelong() {
            return Stream.of(
                    arguments("Observador", Role.OBSERVER, AccountStatus.ACTIVE),
                    arguments("Administrador", Role.ADMINISTRATOR, AccountStatus.ACTIVE),
                    arguments("conta desativada", Role.MEMBER, AccountStatus.DEACTIVATED));
        }

        @ParameterizedTest(name = "{0} não pertence a equipas")
        @MethodSource("whoCannotBelong")
        void onlyActiveManagersAndMembersBelongToTeams(String who, Role role, AccountStatus status) {
            givenAccount(role, status);
            assertThat(refused(() -> teams.addMember(ANA, NORTE, PEDRO_ID)).getCode()).isEqualTo(ProblemCode.INVALID_TEAM_MEMBER);
            verify(queries, never()).addMember(anyLong(), anyLong());
        }

        @Test
        void managerAndMemberWithActiveAccountsAreAdded() {
            givenAccount(Role.MANAGER, AccountStatus.ACTIVE);
            teams.addMember(ANA, COMPRAS, PEDRO_ID);
            verify(queries).addMember(COMPRAS, PEDRO_ID);
        }

        @Test
        void samePersonIsNotAddedTwiceAlsoWhenTwoRequestsArriveTogether() {
            givenAccount(Role.MEMBER, AccountStatus.ACTIVE);
            when(queries.isActiveMember(NORTE, PEDRO_ID)).thenReturn(true);
            assertThat(refused(() -> teams.addMember(ANA, NORTE, PEDRO_ID)).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_MEMBER);
            doThrow(new DuplicateKeyException("ux_team_members_active")).when(queries).addMember(COMPRAS, PEDRO_ID);
            assertThat(refused(() -> teams.addMember(ANA, COMPRAS, PEDRO_ID)).getCode()).isEqualTo(ProblemCode.DUPLICATE_TEAM_MEMBER);
        }

        @Test
        void removingChecksTheTeamThenThePermissionThenTheMembership() {
            // Regressão de F2: a permissão vem antes de saber se a pessoa pertence à equipa.
            assertThat(refused(() -> teams.removeMember(ANA, 99L, PEDRO_ID)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(refused(() -> teams.removeMember(MARTA, COMPRAS, PEDRO_ID)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(queries, never()).isActiveMember(anyLong(), anyLong());
            ApiException error = refused(() -> teams.removeMember(ANA, NORTE, PEDRO_ID));
            assertThat(error.getMessage()).isEqualTo("A pessoa não pertence à equipa.");
        }

        @Test
        void removingMarksTheMembershipAndPublishesTheEvent() {
            when(queries.isActiveMember(NORTE, PEDRO_ID)).thenReturn(true);
            teams.removeMember(ANA, NORTE, PEDRO_ID);
            verify(queries).removeMember(NORTE, PEDRO_ID, clock.now());
            verify(events).publishEvent(new TeamMemberRemoved(NORTE, PEDRO_ID));
        }

        @Test
        void whoBecomesObserverOrAdministratorLeavesAllTeams() {
            when(queries.activeTeamsOf(PEDRO_ID)).thenReturn(List.of(new Reference(NORTE, "Logística Norte"),
                    new Reference(COMPRAS, "Compras")));
            teams.removeFromAllTeams(PEDRO_ID);
            verify(events).publishEvent(new TeamMemberRemoved(NORTE, PEDRO_ID));
            verify(events).publishEvent(new TeamMemberRemoved(COMPRAS, PEDRO_ID));
            verify(queries).removeMember(eq(COMPRAS), eq(PEDRO_ID), any());
        }

        private void givenAccount(Role role, AccountStatus status) {
            when(accounts.findById(PEDRO_ID)).thenReturn(Optional.of(account(PEDRO_ID, "Pedro", role, status)));
        }
    }

    private static AccountView account(long id, String name, Role role, AccountStatus status) {
        return new AccountView(id, name, "p" + id + "@empresa.example", role, status, false);
    }

    private static CurrentUser user(long id, Role role, long... teamIds) {
        List<Reference> references = java.util.Arrays.stream(teamIds).mapToObj(team -> new Reference(team, "Equipa " + team)).toList();
        return new CurrentUser(id, "Pessoa " + id, "p" + id + "@empresa.example", role, references, false);
    }

    private static ApiException refused(Executable call) {
        return assertThrows(ApiException.class, call);
    }
}
