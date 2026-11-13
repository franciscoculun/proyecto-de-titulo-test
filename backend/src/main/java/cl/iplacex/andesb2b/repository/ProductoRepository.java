package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository< Producto, Integer > {
    List< Producto > findByVisibleTrue();
    List< Producto > findByEmpresaVendedoraIdEmpresa(Integer idEmpresa);
    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :idProducto")
    Optional< Producto > findByIdForUpdate(@Param("idProducto") Integer idProducto);
}