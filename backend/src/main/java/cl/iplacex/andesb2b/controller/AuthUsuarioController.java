package cl.iplacex.andesb2b.controller;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.Dtos.UsuarioDto;
import cl.iplacex.andesb2b.model.Usuario;
import cl.iplacex.andesb2b.service.AuthUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthUsuarioController {

    private final AuthUsuarioService authService;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.demo:false}")
    private boolean appDemo;

    public AuthUsuarioController(AuthUsuarioService authService,
                                 SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity< ? > login(@RequestBody Map< String, String > body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        try {
            Usuario u = authService.autenticarEntidad(body.get("email"), body.get("password"));
            String nombreRol = u.getRol().getNombreRol();
            String rolSpring = "ROLE_" + Normalizer.normalize(nombreRol, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toUpperCase();

            UsuarioPrincipal principal = new UsuarioPrincipal(
                    u.getIdUsuario(),
                    u.getEmpresa().getIdEmpresa(),
                    u.getEmail(),
                    u.getNombreCompleto(),
                    nombreRol
            );

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority(rolSpring))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return ResponseEntity.ok(UsuarioDto.from(u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity< ? > logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }

    @GetMapping("/auth/me")
    public ResponseEntity< ? > obtenerSesion(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        if (sesion == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No hay sesión activa."));
        }
        return ResponseEntity.ok(authService.obtenerPerfilActual(sesion.idUsuario()));
    }

    @PostMapping("/auth/registro-empresa")
    public ResponseEntity< ? > registrarEmpresa(@RequestBody Map< String, String > body) {
        try {
            return ResponseEntity.ok(authService.registrarEmpresaYAdmin(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/recuperar")
    public ResponseEntity< ? > recuperar(@RequestBody Map< String, String > body) {
        try {
            String token = authService.generarTokenRecuperacion(body.get("email"));
            Map< String, String > respuesta = new HashMap<>();
            respuesta.put("mensaje", "Solicitud procesada. Ingresa el código de verificación enviado a tu correo corporativo.");
            if (appDemo) {
                respuesta.put("tokenDemo", token);
            }
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/restablecer")
    public ResponseEntity< ? > restablecer(@RequestBody Map< String, String > body) {
        try {
            authService.restablecerPassword(body.get("email"), body.get("token"), body.get("nuevaPassword"));
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida correctamente."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios/mi-empresa")
    public List< UsuarioDto > listarUsuarios(@AuthenticationPrincipal UsuarioPrincipal sesion) {
        return authService.listarPorMiEmpresa(sesion);
    }

    @PostMapping("/usuarios")
    public ResponseEntity< ? > crearUsuario(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                            @RequestBody Map< String, String > body) {
        try {
            return ResponseEntity.ok(authService.crearUsuarioEnMiEmpresa(sesion, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/usuarios/{idUsuario}")
    public ResponseEntity< ? > actualizarUsuario(@AuthenticationPrincipal UsuarioPrincipal sesion,
                                                 @PathVariable Integer idUsuario,
                                                 @RequestBody Map< String, Object > body) {
        try {
            return ResponseEntity.ok(authService.actualizarUsuarioDeMiEmpresa(sesion, idUsuario, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}