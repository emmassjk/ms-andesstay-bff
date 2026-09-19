package cl.duoc.andesstay.bff.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Pagina de resultados. ms-andesstay-audit responde con un Page de Spring
 * Data (con campos extra como pageable/sort); aqui solo se toma lo que el
 * frontend necesita y el resto se ignora, asi el contrato del BFF no depende
 * del formato interno de Spring Data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
