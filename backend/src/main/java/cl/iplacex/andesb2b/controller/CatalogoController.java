package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.Dtos.*;
import cl.iplacex.andesb2b.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/activos")
    public List< ProductoDto > listarActivos() {
        return catalogoService.listarCatalogoActivo();
    }

    @GetMapping("/mi-inventario")
    public ResponseEntity< ? > listarMiInventario(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        try {
            return ResponseEntity.ok(catalogoService.listarMiInventario(sesion));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categorias")
    public List< CategoriaDto > listarCategorias() {
        return catalogoService.listarCategorias();
    }

    @GetMapping("/auditoria")
    public List< LogAuditoriaDto > listarAuditoria(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        return catalogoService.listarAuditoriaMiEmpresa(sesion);
    }

    @PostMapping("/productos")
    public ResponseEntity< ? > crearProducto(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                             @RequestBody Map< String, Object > body) {
        try {
            return ResponseEntity.ok(catalogoService.crearProducto(sesion, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/productos/{idProducto}")
    public ResponseEntity< ? > actualizarProducto(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                                  @PathVariable Integer idProducto,
                                                  @RequestBody Map< String, Object > body) {
        try {
            return ResponseEntity.ok(catalogoService.actualizarProducto(sesion, idProducto, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}