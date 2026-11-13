package cl.iplacex.andesb2b.service;

import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;
    private final DocumentoCobroRepository cobroRepo;

    public PedidoService(PedidoRepository pedidoRepo, DetallePedidoRepository detalleRepo,
                         ProductoRepository productoRepo, EmpresaRepository empresaRepo,
                         UsuarioRepository usuarioRepo, DocumentoCobroRepository cobroRepo) {
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
        this.cobroRepo = cobroRepo;
    }

    @Transactional
    public Map<String, Object> crearPedido(Map<String, Object> payload) {
        Integer idComprador = Integer.valueOf(payload.get("idUsuarioComprador").toString());
        Integer idProducto = Integer.valueOf(payload.get("idProducto").toString());
        Integer cantidad = Integer.valueOf(payload.get("cantidad").toString());

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor a cero.");
        }

        Usuario comprador = usuarioRepo.findById(idComprador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario comprador no válido."));
        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        if (comprador.getEmpresa().getIdEmpresa().equals(prod.getEmpresaVendedora().getIdEmpresa())) {
            throw new IllegalArgumentException("Restricción comercial: Una Pyme no puede emitir pedidos a sí misma.");
        }
        if (prod.getStockDisponible() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para cubrir la cantidad solicitada.");
        }

        Pedido pedido = new Pedido();
        pedido.setEmpresaCompradora(comprador.getEmpresa());
        pedido.setEmpresaVendedora(prod.getEmpresaVendedora());
        pedido.setUsuarioComprador(comprador);
        pedido.setEstadoPedido("COTIZANDO_ENVIO");
        pedido.setCostoEnvio(BigDecimal.ZERO);
        pedido = pedidoRepo.saveAndFlush(pedido);

        DetallePedido det = new DetallePedido();
        det.setPedido(pedido);
        det.setProducto(prod);
        det.setCantidad(cantidad);
        det.setPrecioUnitarioFijo(prod.getPrecioUnitario());
        detalleRepo.saveAndFlush(det);

        return armarRespuestaPedido(pedido);
    }

    @Transactional
    public Map<String, Object> cotizarEnvio(Integer idPedido, Map<String, Object> datos) {
        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        p.setMedioTransporte(datos.get("medioTransporte").toString());
        p.setCostoEnvio(new BigDecimal(datos.get("costoEnvio").toString()));
        p.setEstadoPedido("COTIZANDO_ENVIO");
        return armarRespuestaPedido(pedidoRepo.save(p));
    }

    @Transactional
    public Map<String, Object> responderCotizacion(Integer idPedido, boolean aprobar) {
        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        if (!aprobar) {
            p.setEstadoPedido("RECHAZADO");
            return armarRespuestaPedido(pedidoRepo.save(p));
        }

        List<DetallePedido> detalles = detalleRepo.findByPedidoIdPedido(idPedido);
        BigDecimal subtotalProductos = BigDecimal.ZERO;

        // Descuento atómico de stock y suma de subtotal afecto a comisión
        for (DetallePedido d : detalles) {
            Producto prod = d.getProducto();
            if (prod.getStockDisponible() < d.getCantidad()) {
                throw new IllegalArgumentException("Quiebre de stock para el producto: " + prod.getNombreProducto());
            }
            prod.setStockDisponible(prod.getStockDisponible() - d.getCantidad());
            productoRepo.save(prod);

            BigDecimal linea = d.getPrecioUnitarioFijo().multiply(BigDecimal.valueOf(d.getCantidad()));
            subtotalProductos = subtotalProductos.add(linea);
        }

        p.setEstadoPedido("COMPRA_CONFIRMADA");
        pedidoRepo.save(p);

        // Generación 1:1 en Documentos_Cobro con 3% de comisión (exenta sobre el envío)
        BigDecimal montoEnvio = p.getCostoEnvio() != null ? p.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal comision = subtotalProductos.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOperacion = subtotalProductos.add(montoEnvio);

        DocumentoCobro doc = cobroRepo.findByPedidoIdPedido(idPedido).orElse(new DocumentoCobro());
        doc.setPedido(p);
        doc.setSubtotalProductos(subtotalProductos);
        doc.setMontoEnvio(montoEnvio);
        doc.setComision(comision);
        doc.setTotalOperacion(totalOperacion);
        doc.setEstadoCobro("PENDIENTE_LIQUIDACION");
        cobroRepo.save(doc);

        return armarRespuestaPedido(p);
    }

    @Transactional
    public Map<String, Object> registrarSeguimiento(Integer idPedido, String codigoSeguimiento) {
        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        p.setCodigoSeguimiento(codigoSeguimiento);
        p.setEstadoPedido("EN_TRANSITO");
        return armarRespuestaPedido(pedidoRepo.save(p));
    }

    @Transactional
    public Map<String, Object> confirmarRecepcion(Integer idPedido) {
        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        p.setEstadoPedido("FINALIZADO");
        return armarRespuestaPedido(pedidoRepo.save(p));
    }

public List< Map<String, Object> > listarComprasPorEmpresa(Integer idEmpresa) {
        return pedidoRepo.findByEmpresaCompradoraIdEmpresaOrderByFechaCreacionDesc(idEmpresa)
                .stream().map(this::armarRespuestaPedido).toList();
    }

    public List< Map<String, Object> > listarVentasPorEmpresa(Integer idEmpresa) {
        return pedidoRepo.findByEmpresaVendedoraIdEmpresaOrderByFechaCreacionDesc(idEmpresa)
                .stream().map(this::armarRespuestaPedido).toList();
    }

    public List<DocumentoCobro> listarEstadoCuenta(Integer idEmpresaVendedora) {
        return cobroRepo.findByPedidoEmpresaVendedoraIdEmpresa(idEmpresaVendedora);
    }

    private Map<String, Object> armarRespuestaPedido(Pedido p) {
        List<DetallePedido> detalles = detalleRepo.findByPedidoIdPedido(p.getIdPedido());
        BigDecimal subtotal = detalles.stream()
                .map(d -> d.getPrecioUnitarioFijo().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pedido", p);
        map.put("detalles", detalles);
        map.put("subtotalProductos", subtotal);
        map.put("comisionProyectada", subtotal.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP));
        return map;
    }
}