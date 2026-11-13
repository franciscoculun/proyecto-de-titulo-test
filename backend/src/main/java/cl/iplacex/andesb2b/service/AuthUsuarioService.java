package cl.iplacex.andesb2b.service;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.Dtos.UsuarioDto;
import cl.iplacex.andesb2b.model.Empresa;
import cl.iplacex.andesb2b.model.Rol;
import cl.iplacex.andesb2b.model.Usuario;
import cl.iplacex.andesb2b.repository.EmpresaRepository;
import cl.iplacex.andesb2b.repository.RolRepository;
import cl.iplacex.andesb2b.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final EmpresaRepository empresaRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    public AuthUsuarioService(UsuarioRepository usuarioRepo, EmpresaRepository empresaRepo,
                              RolRepository rolRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.empresaRepo = empresaRepo;
        this.rolRepo = rolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean esEmailValido(String email) {
        return email != null && EMAIL_REGEX.matcher(email.trim()).matches();
    }

    public boolean esRutValido(String rut) {
        if (rut == null) return false;
        String limpio = rut.replace(".", "").replace("-", "").trim().toUpperCase();
        if (limpio.length() < 8 || limpio.length() > 9) return false;

        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dvIngresado = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.matches("\\d+")) return false;

        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resto = 11 - (suma % 11);
        char dvEsperado = (resto == 11) ? '0' : (resto == 10) ? 'K' : (char) ('0' + resto);
        return dvIngresado == dvEsperado;
    }

    @Transactional(readOnly = true)
    public Usuario autenticarEntidad(String email, String password) {
        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("El formato del correo electrónico no es válido.");
        }
        Usuario u = usuarioRepo.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas o correo no registrado."));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new IllegalArgumentException("La cuenta de usuario se encuentra suspendida.");
        }
        if (password == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfilActual(Integer idUsuario) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no válida."));
        return UsuarioDto.from(u);
    }

    @Transactional
    public UsuarioDto registrarEmpresaYAdmin(Map< String, String > datos) {
        String rutEmpresa = datos.get("rutEmpresa");
        String rutUsuario = datos.get("rutUsuario");
        String email = datos.get("email") != null ? datos.get("email").trim().toLowerCase() : "";

        if (!esRutValido(rutEmpresa)) {
            throw new IllegalArgumentException("El RUT de la empresa no es válido (Módulo 11).");
        }
        if (!esRutValido(rutUsuario)) {
            throw new IllegalArgumentException("El RUT del representante no es válido (Módulo 11).");
        }
        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("El correo electrónico debe incluir '@' y un dominio válido.");
        }
        if (empresaRepo.existsByRutEmpresa(rutEmpresa)) {
            throw new IllegalArgumentException("El RUT de la empresa ya se encuentra registrado.");
        }
        if (usuarioRepo.existsByRutUsuario(rutUsuario)) {
            throw new IllegalArgumentException("El RUT del representante ya se encuentra registrado.");
        }
        if (usuarioRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso.");
        }

        Empresa emp = new Empresa();
        emp.setRutEmpresa(rutEmpresa);
        emp.setRazonSocial(datos.get("razonSocial"));
        emp.setGiro(datos.get("giro"));
        emp.setRegion(datos.get("region"));
        emp.setDireccion(datos.get("direccion"));
        emp = empresaRepo.save(emp);

        Rol rolAdmin = rolRepo.findByNombreRol("Administrador")
                .orElseThrow(() -> new IllegalStateException("Rol Administrador no existe en la base de datos."));

        Usuario admin = new Usuario();
        admin.setEmpresa(emp);
        admin.setRol(rolAdmin);
        admin.setRutUsuario(rutUsuario);
        admin.setNombreCompleto(datos.get("nombreCompleto"));
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(datos.get("password")));
        admin.setActivo(true);

        return UsuarioDto.from(usuarioRepo.save(admin));
    }

    @Transactional
    public String generarTokenRecuperacion(String email) {
        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }
        Usuario u = usuarioRepo.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta asociada a ese correo."));
        String token = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        u.setTokenRecuperacion(token);
        usuarioRepo.save(u);
        return token;
    }

    @Transactional
    public void restablecerPassword(String email, String token, String nuevaPassword) {
        Usuario u = usuarioRepo.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Correo no encontrado."));
        if (u.getTokenRecuperacion() == null || !u.getTokenRecuperacion().equals(token)) {
            throw new IllegalArgumentException("Código de verificación inválido o expirado.");
        }
        u.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        u.setTokenRecuperacion(null);
        usuarioRepo.save(u);
    }

    @Transactional(readOnly = true)
    public List< UsuarioDto > listarPorMiEmpresa(UsuarioPrincipal sesion) {
        return usuarioRepo.findByEmpresaIdEmpresa(sesion.idEmpresa())
                .stream().map(UsuarioDto::from).toList();
    }

    @Transactional
    public UsuarioDto crearUsuarioEnMiEmpresa(UsuarioPrincipal sesion, Map< String, String > datos) {
        String rutUsuario = datos.get("rutUsuario");
        String email = datos.get("email") != null ? datos.get("email").trim().toLowerCase() : "";

        if (!esRutValido(rutUsuario)) {
            throw new IllegalArgumentException("El RUT del colaborador no es válido (Módulo 11).");
        }
        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        }
        if (usuarioRepo.existsByRutUsuario(rutUsuario)) {
            throw new IllegalArgumentException("El RUT del colaborador ya está registrado.");
        }
        if (usuarioRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }

        Empresa emp = empresaRepo.findById(sesion.idEmpresa())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        Rol rol = rolRepo.findByNombreRol(datos.get("nombreRol"))
                .orElseThrow(() -> new IllegalArgumentException("Rol no válido."));

        Usuario u = new Usuario();
        u.setEmpresa(emp);
        u.setRol(rol);
        u.setRutUsuario(rutUsuario);
        u.setNombreCompleto(datos.get("nombreCompleto"));
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(datos.getOrDefault("password", "Andes2026!")));
        u.setActivo(true);
        return UsuarioDto.from(usuarioRepo.save(u));
    }

    @Transactional
    public UsuarioDto actualizarUsuarioDeMiEmpresa(UsuarioPrincipal sesion, Integer idUsuario, Map< String, Object > datos) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!u.getEmpresa().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No tienes permiso para modificar usuarios de otra empresa.");
        }
        if (datos.containsKey("nombreRol")) {
            Rol rol = rolRepo.findByNombreRol(datos.get("nombreRol").toString())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
            u.setRol(rol);
        }
        if (datos.containsKey("activo")) {
            u.setActivo(Boolean.valueOf(datos.get("activo").toString()));
        }
        return UsuarioDto.from(usuarioRepo.save(u));
    }
}