package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository< Producto, Integer > {
    List< Producto > findByVisibleTrue();
    List< Producto > findByEmpresaVendedoraIdEmpresa(Integer idEmpresa);
    boolean existsBySku(String sku);
}