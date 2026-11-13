package cl.iplacex.andesb2b.service;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.model.Dtos.*;
import cl.iplacex.andesb2b.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;
    private final DocumentoCobroRepository cobroRepo;

    public PedidoService(PedidoRepository pedidoRepo,
                         DetallePedidoRepository detalleRepo,
                         ProductoRepository productoRepo,
                         UsuarioRepository usuarioRepo,
                         DocumentoCobroRepository cobroRepo) {
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
        this.cobroRepo = cobroRepo;
    }

    private void exigirRolComprador(UsuarioPrincipal sesion) {
        if (!"Comprador".equals(sesion.rol()) && !"Administrador".equals(sesion.rol())) {
            throw new IllegalArgumentException("Tu rol no tiene permisos para realizar operaciones de compra.");
        }
    }

    private void exigirRolVendedor(UsuarioPrincipal sesion) {
        if (!"Vendedor".equals(sesion.rol()) && !"Administrador".equals(sesion.rol())) {
            throw new IllegalArgumentException("Tu rol no tiene permisos para gestionar ventas y despachos.");
        }
    }

    @Transactional
    public PedidoResumenDto crearPedido(UsuarioPrincipal sesion, Map< String, Object > payload) {
        exigirRolComprador(sesion);

        Integer idProducto = Integer.valueOf(payload.get("idProducto").toString());
        Integer cantidad = Integer.valueOf(payload.get("cantidad").toString());

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor a cero.");
        }

        Usuario comprador = usuarioRepo.findById(sesion.idUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Usuario comprador no válido."));
        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        if (!Boolean.TRUE.equals(prod.getVisible())) {
            throw new IllegalArgumentException("El producto seleccionado no se encuentra disponible para la venta.");
        }
        if (comprador.getEmpresa().getIdEmpresa().equals(prod.getEmpresaVendedora().getIdEmpresa())) {
            throw new IllegalArgumentException("Restricción comercial: Una empresa no puede emitir órdenes de compra a sí misma.");
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

        return armarDto(pedido);
    }

    @Transactional
    public PedidoResumenDto cotizarEnvio(UsuarioPrincipal sesion, Integer idPedido, Map< String, Object > datos) {
        exigirRolVendedor(sesion);

        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        if (!p.getEmpresaVendedora().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No puedes cotizar un pedido dirigido a otra empresa proveedora.");
        }
        if (!"COTIZANDO_ENVIO".equals(p.getEstadoPedido())) {
            throw new IllegalArgumentException("Solo se pueden cotizar pedidos que estén en estado COTIZANDO_ENVIO.");
        }

        String transporte = datos.get("medioTransporte") != null ? datos.get("medioTransporte").toString().trim() : "";
        BigDecimal costo = new BigDecimal(datos.get("costoEnvio").toString());
        if (transporte.isEmpty() || costo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Debes indicar una empresa transportista válida y un costo de envío no negativo.");
        }

        p.setMedioTransporte(transporte);
        p.setCostoEnvio(costo);
        return armarDto(pedidoRepo.save(p));
    }

    @Transactional
    public PedidoResumenDto responderCotizacion(UsuarioPrincipal sesion, Integer idPedido, boolean aprobar) {
        exigirRolComprador(sesion);

        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        if (!p.getEmpresaCompradora().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No tienes permiso para responder órdenes de compra de otra empresa.");
        }
        if (!"COTIZANDO_ENVIO".equals(p.getEstadoPedido())) {
            throw new IllegalArgumentException("Esta orden ya fue procesada anteriormente (Estado actual: " + p.getEstadoPedido() + ").");
        }
        if (p.getMedioTransporte() == null || p.getMedioTransporte().isBlank()) {
            throw new IllegalArgumentException("El proveedor aún no ha enviado la cotización de transporte.");
        }

        if (!aprobar) {
            p.setEstadoPedido("RECHAZADO");
            return armarDto(pedidoRepo.save(p));
        }

        List< DetallePedido > detalles = detalleRepo.findByPedidoIdPedido(idPedido);
        BigDecimal subtotalProductos = BigDecimal.ZERO;

        // Bloqueo pesimista (SELECT ... FOR UPDATE) por cada producto antes de descontar stock
        for (DetallePedido d : detalles) {
            Producto prodBloqueado = productoRepo.findByIdForUpdate(d.getProducto().getIdProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

            if (prodBloqueado.getStockDisponible() < d.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente para confirmar la compra de: " + prodBloqueado.getNombreProducto());
            }
            prodBloqueado.setStockDisponible(prodBloqueado.getStockDisponible() - d.getCantidad());
            productoRepo.save(prodBloqueado);

            BigDecimal linea = d.getPrecioUnitarioFijo().multiply(BigDecimal.valueOf(d.getCantidad()));
            subtotalProductos = subtotalProductos.add(linea);
        }

        p.setEstadoPedido("COMPRA_CONFIRMADA");
        pedidoRepo.save(p);

        BigDecimal montoEnvio = p.getCostoEnvio() != null ? p.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal comision = subtotalProductos.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOperacion = subtotalProductos.add(montoEnvio);

        DocumentoCobro doc = new DocumentoCobro();
        doc.setPedido(p);
        doc.setSubtotalProductos(subtotalProductos);
        doc.setMontoEnvio(montoEnvio);
        doc.setComision(comision);
        doc.setTotalOperacion(totalOperacion);
        doc.setEstadoCobro("PENDIENTE_LIQUIDACION");
        cobroRepo.save(doc);

        return armarDto(p);
    }

    @Transactional
    public PedidoResumenDto registrarSeguimiento(UsuarioPrincipal sesion, Integer idPedido, String codigoSeguimiento) {
        exigirRolVendedor(sesion);

        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        if (!p.getEmpresaVendedora().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No puedes gestionar despachos de otra empresa proveedora.");
        }
        if (!"COMPRA_CONFIRMADA".equals(p.getEstadoPedido())) {
            throw new IllegalArgumentException("Solo se puede pasar a EN_TRANSITO un pedido en estado COMPRA_CONFIRMADA.");
        }
        if (codigoSeguimiento == null || codigoSeguimiento.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar un código de seguimiento válido.");
        }

        p.setCodigoSeguimiento(codigoSeguimiento.trim());
        p.setEstadoPedido("EN_TRANSITO");
        return armarDto(pedidoRepo.save(p));
    }

    @Transactional
    public PedidoResumenDto confirmarRecepcion(UsuarioPrincipal sesion, Integer idPedido) {
        exigirRolComprador(sesion);

        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        if (!p.getEmpresaCompradora().getIdEmpresa().equals(sesion.idEmpresa())) {
            throw new IllegalArgumentException("No puedes confirmar la recepción de órdenes de otra empresa.");
        }
        if (!"EN_TRANSITO".equals(p.getEstadoPedido())) {
            throw new IllegalArgumentException("Solo se puede finalizar un pedido que se encuentre EN_TRANSITO.");
        }

        p.setEstadoPedido("FINALIZADO");
        return armarDto(pedidoRepo.save(p));
    }

    @Transactional(readOnly = true)
    public List< PedidoResumenDto > listarMisCompras(UsuarioPrincipal sesion) {
        return pedidoRepo.findByEmpresaCompradoraIdEmpresaOrderByFechaCreacionDesc(sesion.idEmpresa())
                .stream().map(this::armarDto).toList();
    }

    @Transactional(readOnly = true)
    public List< PedidoResumenDto > listarMisVentas(UsuarioPrincipal sesion) {
        return pedidoRepo.findByEmpresaVendedoraIdEmpresaOrderByFechaCreacionDesc(sesion.idEmpresa())
                .stream().map(this::armarDto).toList();
    }

    @Transactional(readOnly = true)
    public List< DocumentoCobroDto > listarMiEstadoCuenta(UsuarioPrincipal sesion) {
        return cobroRepo.findByPedidoEmpresaVendedoraIdEmpresa(sesion.idEmpresa())
                .stream().map(DocumentoCobroDto::from).toList();
    }

    private PedidoResumenDto armarDto(Pedido p) {
        List< DetallePedido > detalles = detalleRepo.findByPedidoIdPedido(p.getIdPedido());
        return PedidoResumenDto.from(p, detalles);
    }
}