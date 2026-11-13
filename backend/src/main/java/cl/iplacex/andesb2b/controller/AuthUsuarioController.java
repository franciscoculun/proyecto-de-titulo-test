package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.model.Rol;
import cl.iplacex.andesb2b.model.Usuario;
import cl.iplacex.andesb2b.service.AuthUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthUsuarioController {

    private final AuthUsuarioService authService;

    public AuthUsuarioController(AuthUsuarioService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            Usuario u = authService.login(body.get("email"), body.get("password"));
            return ResponseEntity.ok(u);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/registro-empresa")
    public ResponseEntity<?> registrarEmpresa(@RequestBody Map<String, String> body) {
        try {
            Usuario admin = authService.registrarEmpresaYAdmin(body);
            return ResponseEntity.ok(admin);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/recuperar")
    public ResponseEntity<?> recuperar(@RequestBody Map<String, String> body) {
        try {
            String token = authService.generarTokenRecuperacion(body.get("email"));
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Enlace temporal generado y enviado al correo corporativo.",
                    "tokenDemo", token
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/restablecer")
    public ResponseEntity<?> restablecer(@RequestBody Map<String, String> body) {
        try {
            authService.restablecerPassword(body.get("email"), body.get("token"), body.get("nuevaPassword"));
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida correctamente."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios/empresa/{idEmpresa}")
    public List<Usuario> listarUsuarios(@PathVariable Integer idEmpresa) {
        return authService.listarPorEmpresa(idEmpresa);
    }

    @GetMapping("/roles")
    public List<Rol> listarRoles() {
        return authService.listarRoles();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.crearUsuarioPyme(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/usuarios/{idUsuario}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Integer idUsuario, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(authService.actualizarUsuario(idUsuario, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}