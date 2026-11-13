package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository< Usuario, Integer > {
    Optional< Usuario > findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRutUsuario(String rutUsuario);
    List< Usuario > findByEmpresaIdEmpresa(Integer idEmpresa);
}