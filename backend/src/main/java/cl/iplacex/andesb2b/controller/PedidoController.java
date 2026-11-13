package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.Dtos.*;
import cl.iplacex.andesb2b.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity< ? > crearPedido(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                           @RequestBody Map< String, Object > body) {
        try {
            return ResponseEntity.ok(pedidoService.crearPedido(sesion, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mis-compras")
    public List< PedidoResumenDto > listarMisCompras(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        return pedidoService.listarMisCompras(sesion);
    }

    @GetMapping("/mis-ventas")
    public List< PedidoResumenDto > listarMisVentas(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        return pedidoService.listarMisVentas(sesion);
    }

    @PutMapping("/{idPedido}/cotizar")
    public ResponseEntity< ? > cotizarEnvio(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                            @PathVariable Integer idPedido,
                                            @RequestBody Map< String, Object > body) {
        try {
            return ResponseEntity.ok(pedidoService.cotizarEnvio(sesion, idPedido, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/respuesta")
    public ResponseEntity< ? > responderCotizacion(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                                   @PathVariable Integer idPedido,
                                                   @RequestBody Map< String, Boolean > body) {
        try {
            boolean aprobar = Boolean.TRUE.equals(body.get("aprobar"));
            return ResponseEntity.ok(pedidoService.responderCotizacion(sesion, idPedido, aprobar));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/seguimiento")
    public ResponseEntity< ? > registrarSeguimiento(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                                    @PathVariable Integer idPedido,
                                                    @RequestBody Map< String, String > body) {
        try {
            return ResponseEntity.ok(pedidoService.registrarSeguimiento(sesion, idPedido, body.get("codigoSeguimiento")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idPedido}/recepcion")
    public ResponseEntity< ? > confirmarRecepcion(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                                  @PathVariable Integer idPedido) {
        try {
            return ResponseEntity.ok(pedidoService.confirmarRecepcion(sesion, idPedido));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mi-estado-cuenta")
    public List< DocumentoCobroDto > listarMiEstadoCuenta(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        return pedidoService.listarMiEstadoCuenta(sesion);
    }
}