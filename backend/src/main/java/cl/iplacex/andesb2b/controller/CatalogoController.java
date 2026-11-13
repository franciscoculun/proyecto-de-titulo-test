package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.model.Categoria;
import cl.iplacex.andesb2b.model.LogAuditoriaPrecio;
import cl.iplacex.andesb2b.model.Producto;
import cl.iplacex.andesb2b.service.CatalogoService;
import org.springframework.http.ResponseEntity;
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
    public List<Producto> listarActivos() {
        return catalogoService.listarCatalogoActivo();
    }

    @GetMapping("/empresa/{idEmpresa}")
    public List<Producto> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        return catalogoService.listarPorEmpresa(idEmpresa);
    }

    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return catalogoService.listarCategorias();
    }

    @GetMapping("/auditoria")
    public List<LogAuditoriaPrecio> listarAuditoria() {
        return catalogoService.listarAuditoria();
    }

    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(catalogoService.crearProducto(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/productos/{idProducto}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Integer idProducto, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(catalogoService.actualizarProducto(idProducto, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}