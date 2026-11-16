package pt.matriz.projects;

/** Estado do projeto (requirements.md, secção 4.1; nomes técnicos de DA08). */
public enum ProjectStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED;

    /** Transições permitidas (RN07, requirements.md secção 4.1). */
    public boolean canChangeTo(ProjectStatus target) {
        return switch (this) {
            case ACTIVE -> target == COMPLETED;
            case COMPLETED -> target == ACTIVE || target == ARCHIVED;
            case ARCHIVED -> target == COMPLETED;
        };
    }
}
