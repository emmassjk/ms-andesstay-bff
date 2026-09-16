package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.CatalogClient;
import cl.duoc.andesstay.bff.dto.UnitDto;
import cl.duoc.andesstay.bff.dto.UnitRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Puerta de entrada del frontend Angular hacia el dominio de catalogo.
 * No contiene logica de negocio: valida y reenvia hacia
 * ms-andesstay-catalog a traves de CatalogClient.
 *
 * La autenticacion (JWT valido) y autorizacion (rol ADMIN/CLIENTE) de estas
 * rutas ya se resuelven antes de llegar aqui, en SecurityConfig; si esta
 * clase se ejecuta, es porque el token era valido y el rol alcanzaba.
 */
@RestController
@RequestMapping("/api/catalog/units")
public class CatalogProxyController {

    private final CatalogClient catalogClient;

    public CatalogProxyController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @GetMapping
    public List<UnitDto> getAll() {
        return catalogClient.findAll();
    }

    @GetMapping("/{id}")
    public UnitDto getById(@PathVariable Long id) {
        return catalogClient.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitDto create(@Valid @RequestBody UnitRequestDto request) {
        return catalogClient.create(request);
    }

    @PutMapping("/{id}")
    public UnitDto update(@PathVariable Long id, @Valid @RequestBody UnitRequestDto request) {
        return catalogClient.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalogClient.delete(id);
        return ResponseEntity.noContent().build();
    }
}
