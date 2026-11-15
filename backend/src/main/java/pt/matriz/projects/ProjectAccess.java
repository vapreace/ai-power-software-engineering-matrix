package pt.matriz.projects;

/**
 * O que uma pessoa pode fazer num projeto, para os outros módulos (por exemplo, Tarefas).
 * {@code visible}: vê o projeto e o que lhe pertence (requirements.md, secção 2).
 * {@code manageable}: é o Administrador ou o gestor do projeto (RN05, RN22).
 * Não diz nada sobre o estado: o módulo que chama decide com {@code status} (RN08, RN09).
 */
public record ProjectAccess(long projectId, long teamId, ProjectStatus status, boolean visible, boolean manageable) {

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }
}
