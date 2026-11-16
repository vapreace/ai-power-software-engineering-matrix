package pt.matriz.accounts.api;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import pt.matriz.common.Patch;
import pt.matriz.common.Role;

/** PATCH /users/{id}: só os campos enviados mudam. Campos desconhecidos são recusados (400). */
class UpdateUserRequest {

    private Patch<String> name = Patch.absent();
    private Patch<String> email = Patch.absent();
    private Patch<Role> role = Patch.absent();
    private final Set<String> unknownFields = new LinkedHashSet<>();

    public void setName(String name) {
        this.name = Patch.of(name);
    }

    public void setEmail(String email) {
        this.email = Patch.of(email);
    }

    public void setRole(Role role) {
        this.role = Patch.of(role);
    }

    @JsonAnySetter
    public void setUnknownField(String field, Object value) {
        unknownFields.add(field);
    }

    Patch<String> name() {
        return name;
    }

    Patch<String> email() {
        return email;
    }

    Patch<Role> role() {
        return role;
    }

    Set<String> unknownFields() {
        return Set.copyOf(unknownFields);
    }
}
