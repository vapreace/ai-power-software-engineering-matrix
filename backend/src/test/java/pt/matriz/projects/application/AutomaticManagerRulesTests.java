package pt.matriz.projects.application;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.matriz.accounts.AccountDeactivated;
import pt.matriz.accounts.RoleChanged;
import pt.matriz.common.Role;
import pt.matriz.projects.Projects;
import pt.matriz.teams.TeamMemberRemoved;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Testes unitários de {@link ManagerRulesListener} (passo 2b, DA37): o projeto fica "Sem gestor"
 * quando o gestor deixa de cumprir RN21 (RN22, RN23, architecture.md secção 7).
 */
@ExtendWith(MockitoExtension.class)
class AutomaticManagerRulesTests {

    @Mock
    private Projects projects;

    private ManagerRulesListener listener;

    @BeforeEach
    void setUp() {
        listener = new ManagerRulesListener(projects);
    }

    @Test
    void deactivatedManagerLeavesAllTheirProjectsWithoutManager() {
        listener.on(new AccountDeactivated(2L));
        verify(projects).clearManager(2L, null);
    }

    @Test
    void managerWhoLeavesATeamLeavesOnlyTheProjectsOfThatTeam() {
        listener.on(new TeamMemberRemoved(10L, 2L));
        verify(projects).clearManager(2L, 10L);
    }

    static Stream<Arguments> roleChanges() {
        return Stream.of(
                arguments(Role.MANAGER, Role.MEMBER, true),
                arguments(Role.MANAGER, Role.OBSERVER, true),
                arguments(Role.MANAGER, Role.ADMINISTRATOR, true),
                arguments(Role.MANAGER, Role.MANAGER, false),
                arguments(Role.MEMBER, Role.MANAGER, false),
                arguments(Role.MEMBER, Role.OBSERVER, false),
                arguments(Role.ADMINISTRATOR, Role.MANAGER, false));
    }

    @ParameterizedTest(name = "{0} para {1}: deixa os projetos sem gestor = {2}")
    @MethodSource("roleChanges")
    void onlyWhoStopsBeingManagerLeavesTheProjectsWithoutManager(Role previous, Role next, boolean clears) {
        listener.on(new RoleChanged(2L, previous, next));
        if (clears) {
            verify(projects).clearManager(2L, null);
        } else {
            verify(projects, never()).clearManager(anyLong(), any());
        }
    }
}
