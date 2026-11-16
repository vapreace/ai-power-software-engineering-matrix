package pt.matriz.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pt.matriz.tasks.TaskTotals;

/**
 * Testes unitários do progresso de um projeto (passo 2b, DA37; requirements.md secção 2, US09, E1):
 * concluídas ÷ total × 100, arredondado para baixo; sem tarefas, não há percentagem ("Sem tarefas").
 */
class ProgressRulesTests {

    @ParameterizedTest(name = "{0} de {1} concluídas: {2}")
    @CsvSource(nullValues = "sem", value = {
            "0, 0, sem",
            "0, 4, 0",
            "6, 10, 60",
            "2, 3, 66",
            "1, 3, 33",
            "10, 10, 100"})
    void percentageIsRoundedDown(long done, long total, Integer percentage) {
        Progress progress = Progress.of(new TaskTotals(total, total - done, 0, done, 0));
        assertThat(progress.percentage()).isEqualTo(percentage);
        assertThat(progress.completedTasks()).isEqualTo(done);
        assertThat(progress.totalTasks()).isEqualTo(total);
    }
}
