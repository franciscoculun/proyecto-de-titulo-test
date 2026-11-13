package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.model.DocumentoCobro;
import cl.iplacex.andesb2b.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(pedidoService.crearPedido(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/compras/{idEmpresa}")
    public List< Map<String, Object> > listarCompras(@PathVariable Integer idEmpresa) {
        return pedidoService.listarComprasPorEmpresa(idEmpresa);
    }

    @GetMapping("/ventas/{idEmpresa}")
    public List< Map<String, Object> > listarVentas(@PathVariable Integer idEmpresa) {
        return pedidoService.listarVentasPorEmpresa(idEmpresa);
    }

    @PutMapping("/{idPedido}/cotizar")
    public ResponseEntity<?> cotizarEnvio(@PathVariable Integer idPedido, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(pedidoService.cotizarEnvio(idPedido, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/respuesta")
    public ResponseEntity<?> responderCotizacion(@PathVariable Integer idPedido, @RequestBody Map<String, Boolean> body) {
        try {
            boolean aprobar = Boolean.TRUE.equals(body.get("aprobar"));
            return ResponseEntity.ok(pedidoService.responderCotizacion(idPedido, aprobar));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/seguimiento")
    public ResponseEntity<?> registrarSeguimiento(@PathVariable Integer idPedido, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(pedidoService.registrarSeguimiento(idPedido, body.get("codigoSeguimiento")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/recepcion")
    public ResponseEntity<?> confirmarRecepcion(@PathVariable Integer idPedido) {
        try {
            return ResponseEntity.ok(pedidoService.confirmarRecepcion(idPedido));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estado-cuenta/{idEmpresa}")
    public List<DocumentoCobro> listarEstadoCuenta(@PathVariable Integer idEmpresa) {
        return pedidoService.listarEstadoCuenta(idEmpresa);
    }
}