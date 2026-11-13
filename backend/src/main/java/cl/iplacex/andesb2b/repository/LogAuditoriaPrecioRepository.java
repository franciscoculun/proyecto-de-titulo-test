package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.LogAuditoriaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogAuditoriaPrecioRepository extends JpaRepository<LogAuditoriaPrecio, Integer> {
    List<LogAuditoriaPrecio> findAllByOrderByFechaModificacionDesc();
}