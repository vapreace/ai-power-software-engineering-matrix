package pt.matriz.common;

import java.util.List;

/** Lista da API dentro de {@code items}, sem paginação (DA18). */
public record Items<T>(List<T> items) {
}
