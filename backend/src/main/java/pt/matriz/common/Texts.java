package pt.matriz.common;

import java.text.Normalizer;

/**
 * Tratamento dos textos escritos pelos utilizadores antes de validar e gravar (DA11):
 * retira os espaços nas pontas e normaliza em Unicode NFC. Os limites contam códigos
 * Unicode, por isso uma letra acentuada conta um (RN11, RN29, LIM13).
 */
public final class Texts {

    private Texts() {
    }

    /** Texto sem espaços nas pontas e normalizado (NFC). {@code null} fica {@code null}. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC).strip();
    }

    /** Número de caracteres, contados como códigos Unicode (DA11). */
    public static int length(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    /** Texto normalizado, ou {@code null} quando fica vazio. Para os campos opcionais. */
    public static String normalizeOptional(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
