package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.DocumentoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentoCobroRepository extends JpaRepository<DocumentoCobro, Integer> {
    Optional<DocumentoCobro> findByPedidoIdPedido(Integer idPedido);
    List<DocumentoCobro> findByPedidoEmpresaVendedoraIdEmpresa(Integer idEmpresa);
}