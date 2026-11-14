package cl.iplacex.andesb2b.service;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.model.Dtos.*;
import cl.iplacex.andesb2b.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CatalogoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
    private final EmpresaRepository empresaRepo;
    private final LogAuditoriaPrecioRepository logRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public CatalogoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo,
                           EmpresaRepository empresaRepo, LogAuditoriaPrecioRepository logRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
        this.empresaRepo = empresaRepo;
        this.logRepo = logRepo;
    }

    private void validarRolVendedorOAdmin(UsuarioPrincipal sesion) {
        if (!"Vendedor".equals(sesion.rol()) && !"Administrador".equals(sesion.rol())) {
            throw new IllegalArgumentException("Solo usuarios con rol Vendedor o Administrador pueden gestionar el catálogo.");
        }
    }

    @Transactional(readOnly = true)
    public List< ProductoDto > listarCatalogoActivo() {
        return productoRepo.findByVisibleTrue().stream().map(ProductoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List< ProductoDto > listarMiInventario(UsuarioPrincipal sesion) {
        validarRolVendedorOAdmin(sesion);
        return productoRepo.findByEmpresaVendedoraIdEmpresa(sesion.idEmpresa())
                .stream().map(ProductoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List< CategoriaDto > listarCategorias() {
        return categoriaRepo.findAll().stream().map(CategoriaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List< LogAuditoriaDto > listarAuditoriaMiEmpresa(UsuarioPrincipal sesion) {
        return logRepo.findAllByOrderByFechaModificacionDesc().stream()
                .filter(l -> l.getProducto().getEmpresaVendedora().getIdEmpresa().equals(sesion.idEmpresa()))
                .map(LogAuditoriaDto::from)
                .toList();
    }

    @Transactional
    public ProductoDto crearProducto(UsuarioPrincipal sesion, Map<String, Object> datos) {
        validarRolVendedorOAdmin(sesion);
        String sku = datos.get("sku").toString().trim();
        if (productoRepo.existsBySku(sku)) {
            throw new IllegalArgumentException("El código SKU ya existe en el catálogo.");
        }

        Empresa emp = empresaRepo.findById(sesion.idEmpresa())
                .orElseThrow(() -> new IllegalArgumentException("Empresa vendedora no encontrada."));
        Categoria cat = categoriaRepo.findById(Integer.valueOf(datos.get("idCategoria").toString()))
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));

        BigDecimal precio = new BigDecimal(datos.get("precioUnitario").toString());
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }

        // Si no se ingresa stock (nulo o vacío), se asigna 0 por defecto
        int stock = 0;
        Object stockRaw = datos.get("stockDisponible");
        if (stockRaw != null && !stockRaw.toString().trim().isEmpty()) {
            stock = Integer.parseInt(stockRaw.toString().trim());
            if (stock < 0) {
                throw new IllegalArgumentException("El stock disponible no puede ser negativo.");
            }
        }

        // Si no se indica visibilidad, nace como borrador (false) por defecto
        boolean visible = datos.get("visible") != null
                && Boolean.parseBoolean(datos.get("visible").toString());

        Producto p = new Producto();
        p.setEmpresaVendedora(emp);
        p.setCategoria(cat);
        p.setSku(sku);
        p.setNombreProducto(datos.get("nombreProducto").toString().trim());
        p.setPrecioUnitario(precio);
        p.setStockDisponible(stock);
        p.setVisible(visible);

        return ProductoDto.from(productoRepo.save(p));
    }

    @Transactional
    public ProductoDto actualizarProducto(UsuarioPrincipal sesion, Integer idProducto, Map< String, Object > datos) {
        validarRolVendedorOAdmin(sesion);

        Producto p = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        if (!p.getEmpresaVendedora().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No puedes modificar productos pertenecientes a otra empresa.");
        }

        if ( entityManager != null ) {
            entityManager.createNativeQuery("SELECT set_config('app.id_usuario', :uid, true)")
                    .setParameter("uid", String.valueOf(sesion.idUsuario()))
                    .getSingleResult();
        }

        if (datos.containsKey("nombreProducto")) {
            p.setNombreProducto(datos.get("nombreProducto").toString().trim());
        }
        if (datos.containsKey("precioUnitario")) {
            BigDecimal nuevoPrecio = new BigDecimal(datos.get("precioUnitario").toString());
            if (nuevoPrecio.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo.");
            }
            p.setPrecioUnitario(nuevoPrecio);
        }
        if (datos.containsKey("stockDisponible")) {
            Integer nuevoStock = Integer.valueOf(datos.get("stockDisponible").toString());
            if (nuevoStock < 0) {
                throw new IllegalArgumentException("El stock no puede ser negativo.");
            }
            p.setStockDisponible(nuevoStock);
        }
        if (datos.containsKey("visible")) {
            p.setVisible(Boolean.valueOf(datos.get("visible").toString()));
        }

        return ProductoDto.from(productoRepo.saveAndFlush(p));
    }
}