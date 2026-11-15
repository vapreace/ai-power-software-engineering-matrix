package pt.matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Confirma que o Liquibase cria o esquema de docs/database.md num PostgreSQL real (DA30, DA31). */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class DatabaseSchemaTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void liquibaseCreatesAllTables() {
        List<String> tables = jdbc.queryForList("""
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public' AND tablename NOT LIKE 'databasechangelog%'
                ORDER BY tablename
                """, String.class);

        assertThat(tables).containsExactly(
                "projects", "task_assignees", "task_comments", "tasks", "team_members", "teams", "users");
    }

    @Test
    void demoDataIsOnlyLoadedInTheDemoProfile() {
        Integer demoChangeSets = jdbc.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id LIKE '100-demo%'", Integer.class);
        assertThat(demoChangeSets).isZero();
    }

    @Test
    void searchAndOrderingIgnoreCaseAndAccents() {
        assertThat(jdbc.queryForObject("SELECT unaccent(lower('Armazém'))", String.class)).isEqualTo("armazem");

        List<String> ordered = jdbc.queryForList("""
                SELECT name FROM (VALUES ('ótica'), ('Obras'), ('orçamento')) AS v(name)
                ORDER BY name COLLATE matriz_ci_ai
                """, String.class);
        assertThat(ordered).containsExactly("Obras", "orçamento", "ótica");
    }
}
