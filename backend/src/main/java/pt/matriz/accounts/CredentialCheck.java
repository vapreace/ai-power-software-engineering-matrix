package pt.matriz.accounts;

/**
 * Resultado de verificar email e palavra-passe. {@code DEACTIVATED} só aparece quando
 * a palavra-passe está correta (SEG04, SEG05).
 */
public record CredentialCheck(Result result, AccountView account) {

    public enum Result {
        VALID,
        INVALID,
        DEACTIVATED
    }

    static CredentialCheck valid(AccountView account) {
        return new CredentialCheck(Result.VALID, account);
    }

    static CredentialCheck invalid() {
        return new CredentialCheck(Result.INVALID, null);
    }

    static CredentialCheck deactivated() {
        return new CredentialCheck(Result.DEACTIVATED, null);
    }
}
