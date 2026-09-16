package cl.duoc.andesstay.bff.dto;

import java.math.BigDecimal;

/**
 * Representa una unidad (habitacion/cabana/lodge) tal como la devuelve
 * ms-andesstay-catalog. Se mantiene como un DTO propio del BFF (en vez de
 * compartir la entidad JPA del microservicio) para no acoplar los contratos
 * publicos del BFF a los detalles internos de persistencia del catalogo.
 */
public record UnitDto(
        Long id,
        String name,
        String type,
        Integer capacity,
        BigDecimal pricePerNight,
        Boolean available
) {
}
