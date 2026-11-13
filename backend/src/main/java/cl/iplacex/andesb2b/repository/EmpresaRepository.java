package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository< Empresa, Integer > {
    Optional< Empresa > findByRutEmpresa(String rutEmpresa);
    boolean existsByRutEmpresa(String rutEmpresa);
}