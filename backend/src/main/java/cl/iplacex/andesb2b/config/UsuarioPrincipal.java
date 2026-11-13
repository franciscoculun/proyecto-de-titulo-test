package cl.iplacex.andesb2b.config;

import java.io.Serializable;

public record UsuarioPrincipal(
        Integer idUsuario,
        Integer idEmpresa,
        String email,
        String nombreCompleto,
        String rol
) implements Serializable {
}