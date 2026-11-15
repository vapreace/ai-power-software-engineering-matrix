package pt.matriz.common;

/**
 * Valor de um campo num pedido PATCH: distingue o campo ausente (não muda) do campo
 * enviado, mesmo com {@code null} (por exemplo, retirar a data de fim prevista).
 */
public record Patch<T>(boolean present, T value) {

    private static final Patch<?> ABSENT = new Patch<>(false, null);

    @SuppressWarnings("unchecked")
    public static <T> Patch<T> absent() {
        return (Patch<T>) ABSENT;
    }

    public static <T> Patch<T> of(T value) {
        return new Patch<>(true, value);
    }

    /** O valor enviado, ou o atual quando o campo não foi enviado. */
    public T orElse(T current) {
        return present ? value : current;
    }
}
