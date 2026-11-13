package cl.iplacex.andesb2b.service;

import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.repository.*;
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
    private final UsuarioRepository usuarioRepo;
    private final LogAuditoriaPrecioRepository logRepo;

    public CatalogoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo,
                           EmpresaRepository empresaRepo, UsuarioRepository usuarioRepo,
                           LogAuditoriaPrecioRepository logRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.logRepo = logRepo;
    }

    public List<Producto> listarCatalogoActivo() {
        return productoRepo.findByVisibleTrue();
    }

    public List<Producto> listarPorEmpresa(Integer idEmpresa) {
        return productoRepo.findByEmpresaVendedoraIdEmpresa(idEmpresa);
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepo.findAll();
    }

    public List<LogAuditoriaPrecio> listarAuditoria() {
        return logRepo.findAllByOrderByFechaModificacionDesc();
    }

    @Transactional
    public Producto crearProducto(Map<String, Object> datos) {
        String sku = datos.get("sku").toString();
        if (productoRepo.existsBySku(sku)) {
            throw new IllegalArgumentException("El código SKU ya existe en el catálogo.");
        }

        Empresa emp = empresaRepo.findById(Integer.valueOf(datos.get("idEmpresaVendedora").toString()))
                .orElseThrow(() -> new IllegalArgumentException("Empresa vendedora no encontrada."));
        Categoria cat = categoriaRepo.findById(Integer.valueOf(datos.get("idCategoria").toString()))
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));

        BigDecimal precio = new BigDecimal(datos.get("precioUnitario").toString());
        Integer stock = Integer.valueOf(datos.get("stockDisponible").toString());
        if (precio.compareTo(BigDecimal.ZERO) < 0 || stock < 0) {
            throw new IllegalArgumentException("El precio y el stock no pueden ser negativos.");
        }

        Producto p = new Producto();
        p.setEmpresaVendedora(emp);
        p.setCategoria(cat);
        p.setSku(sku);
        p.setNombreProducto(datos.get("nombreProducto").toString());
        p.setPrecioUnitario(precio);
        p.setStockDisponible(stock);
        p.setVisible(Boolean.valueOf(datos.getOrDefault("visible", "false").toString()));

        return productoRepo.save(p);
    }

    @Transactional
    public Producto actualizarProducto(Integer idProducto, Map<String, Object> datos) {
        Producto p = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        BigDecimal precioAnterior = p.getPrecioUnitario();

        if (datos.containsKey("nombreProducto")) {
            p.setNombreProducto(datos.get("nombreProducto").toString());
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

        Producto guardado = productoRepo.saveAndFlush(p);

        // Si cambió el precio y viene idUsuario, asociamos el autor al registro creado por el trigger SQL
        if (datos.containsKey("idUsuario") && p.getPrecioUnitario().compareTo(precioAnterior) != 0) {
            Integer idUsuario = Integer.valueOf(datos.get("idUsuario").toString());
            usuarioRepo.findById(idUsuario).ifPresent(u -> {
                List<LogAuditoriaPrecio> logs = logRepo.findAllByOrderByFechaModificacionDesc();
                logs.stream()
                    .filter(l -> l.getProducto().getIdProducto().equals(idProducto) && l.getUsuario() == null)
                    .findFirst()
                    .ifPresent(l -> {
                        l.setUsuario(u);
                        logRepo.save(l);
                    });
            });
        }
        return guardado;
    }
}