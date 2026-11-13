package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEmpresaCompradoraIdEmpresaOrderByFechaCreacionDesc(Integer idEmpresa);
    List<Pedido> findByEmpresaVendedoraIdEmpresaOrderByFechaCreacionDesc(Integer idEmpresa);
}