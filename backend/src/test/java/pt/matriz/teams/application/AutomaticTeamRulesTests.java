package pt.matriz.teams.application;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.matriz.accounts.RoleChanged;
import pt.matriz.common.Role;
import pt.matriz.teams.Teams;

/**
 * Testes unitários de {@link RoleChangedListener} (passo 2b, DA37): quem passa a Observador ou
 * Administrador deixa de pertencer às suas equipas (RN23, RN26).
 */
@ExtendWith(MockitoExtension.class)
class AutomaticTeamRulesTests {

    @Mock
    private Teams teams;

    static Stream<Arguments> roleChanges() {
        return Stream.of(
                arguments(Role.MEMBER, Role.OBSERVER, true),
                arguments(Role.MANAGER, Role.ADMINISTRATOR, true),
                arguments(Role.MANAGER, Role.MEMBER, false),
                arguments(Role.MEMBER, Role.MANAGER, false));
    }

    @ParameterizedTest(name = "{0} para {1}: sai das equipas = {2}")
    @MethodSource("roleChanges")
    void onlyObserversAndAdministratorsLeaveTheirTeams(Role previous, Role next, boolean leaves) {
        new RoleChangedListener(teams).on(new RoleChanged(6L, previous, next));
        if (leaves) {
            verify(teams).removeFromAllTeams(6L);
        } else {
            verify(teams, never()).removeFromAllTeams(anyLong());
        }
    }
}
